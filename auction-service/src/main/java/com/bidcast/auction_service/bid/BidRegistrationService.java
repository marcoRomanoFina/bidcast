package com.bidcast.auction_service.bid;

import com.bidcast.auction_service.client.WalletClient;
import com.bidcast.auction_service.client.WalletFreezeRequest;
import com.bidcast.auction_service.core.exception.SessionInactiveException;
import com.bidcast.auction_service.core.exception.WalletCommunicationException;
import com.bidcast.auction_service.session.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * servicio para el registro y activación de Pujas en Redis (Única Fuente Hot).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BidRegistrationService {

    private final BidPersistenceService persistenceService;
    private final BidInfrastructureService infrastructureService;
    private final BidRehydrationService rehydrationService;
    private final WalletClient walletClient;
    private final SessionService sessionService;

    // metodo para registrar un bid
    // mini saga (compensitory), ya que estamos tocando db del wallet service, hay que tener en cuenta 
    // y lograr una transaccion distribuida
    public SessionBid registerBid(BidRegistrationRequest request) {
        log.info("Registering bid for advertiser {} in session {}", request.advertiserId(), request.sessionId());

        // si la session no esta activa throw error
        if (!sessionService.isSessionActive(request.sessionId())) {
            throw new SessionInactiveException("The session does not exist or has already been closed");
        }

        // 1. Guardar en DB (Cold Data)
        SessionBid bid = persistenceService.saveAsPending(request);
        String referenceId = "bid:" + bid.getId();

        // 2. Bloqueo de Fondos
        try {
            walletClient.freeze(new WalletFreezeRequest(
                    request.advertiserId(),
                    request.totalBudget(),
                    referenceId,
                    "Reservation for session " + request.sessionId()
            ));
        } catch (Exception e) {
            log.error("Failed to freeze funds for bid {}. Cancelling registration.", bid.getId());
            persistenceService.updateStatus(bid.getId(), BidStatus.FAILED);
            throw new WalletCommunicationException("Wallet service communication failure");
        }

        // 3. Activación e Inyección en Redis (Hot Data)
        try {
            SessionBid activeBid = persistenceService.updateStatus(bid.getId(), BidStatus.ACTIVE);
            
            // Calculamos el saldo real (que en este punto es el totalBudget) e inyectamos
            long balanceCents = rehydrationService.calculateRealBalanceCents(activeBid);
            infrastructureService.injectIntoRedis(activeBid, balanceCents);

            return activeBid;

        } catch (Exception e) {
            log.error("Critical infrastructure failure for bid {}. Running compensation...", bid.getId(), e);
            handleImmediateFailure(bid, referenceId);
            throw new RuntimeException("Internal error: funds were returned to the advertiser.");
        }
    }

    // metodo privado por si tenemos que recompensar y "unfrezzseamos" lo que habiamos freezeado
    private void handleImmediateFailure(SessionBid bid, String referenceId) {
        try {
            walletClient.unfreeze(new WalletFreezeRequest(
                    bid.getAdvertiserId(),
                    bid.getTotalBudget(),
                    referenceId,
                    "Rollback due to infrastructure failure"
            ));
            persistenceService.updateStatus(bid.getId(), BidStatus.FAILED);
        } catch (Exception ex) {
            log.error("CRITICAL ALERT: Failed to unfreeze bid {}. Status: FAILED_CRITICAL", bid.getId());
            persistenceService.updateStatus(bid.getId(), BidStatus.FAILED_CRITICAL);
        }
    }
}
