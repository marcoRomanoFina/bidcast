package com.bidcast.auction_service.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Lanzada cuando ocurre un error al intentar liquidar una puja específica
 * durante el cierre de la sesión.
 */
public class BidSettlementException extends AuctionDomainException {
    public BidSettlementException(String bidId, String reason) {
        super(String.format("Error al liquidar la puja %s: %s", bidId, reason), 
              HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
