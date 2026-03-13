package com.bidcast.auction_service.bid;

import com.bidcast.auction_service.client.WalletClient;
import com.bidcast.auction_service.client.WalletFreezeRequest;
import com.bidcast.auction_service.core.exception.WalletCommunicationException;
import com.bidcast.auction_service.session.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ORQUESTADOR: Registro y Activación de Pujas en Redis (Única Fuente Hot).
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

    public SessionBid registerBid(BidRegistrationRequest request) {
        log.info("Registrando puja para anunciante {} en sesión {}", request.advertiserId(), request.sessionId());

        if (!sessionService.isSessionActive(request.sessionId())) {
            throw new IllegalArgumentException("La sesión no existe o ya fue cerrada");
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
                    "Reserva para sesión " + request.sessionId()
            ));
        } catch (Exception e) {
            log.error("Error al congelar fondos para el bid {}. Cancelando registro.", bid.getId());
            persistenceService.updateStatus(bid.getId(), BidStatus.FAILED);
            throw new WalletCommunicationException("Fallo en la comunicación con el Wallet Service");
        }

        // 3. Activación e Inyección en Redis (Hot Data)
        try {
            SessionBid activeBid = persistenceService.updateStatus(bid.getId(), BidStatus.ACTIVE);
            
            // Calculamos el saldo real (que en este punto es el totalBudget) e inyectamos
            long balanceCents = rehydrationService.calculateRealBalanceCents(activeBid);
            infrastructureService.injectIntoRedis(activeBid, balanceCents);

            return activeBid;

        } catch (Exception e) {
            log.error("Fallo crítico en infraestructura para bid {}. Ejecutando compensación...", bid.getId(), e);
            handleImmediateFailure(bid, referenceId);
            throw new RuntimeException("Error interno: los fondos fueron devueltos al anunciante.");
        }
    }

    private void handleImmediateFailure(SessionBid bid, String referenceId) {
        try {
            walletClient.unfreeze(new WalletFreezeRequest(
                    bid.getAdvertiserId(),
                    bid.getTotalBudget(),
                    referenceId,
                    "Reversión por fallo de infraestructura"
            ));
            persistenceService.updateStatus(bid.getId(), BidStatus.FAILED);
        } catch (Exception ex) {
            log.error("ALERTA ROJA: No se pudo realizar el unfreeze para el bid {}. Estado: FAILED_CRITICAL", bid.getId());
            persistenceService.updateStatus(bid.getId(), BidStatus.FAILED_CRITICAL);
        }
    }
}
