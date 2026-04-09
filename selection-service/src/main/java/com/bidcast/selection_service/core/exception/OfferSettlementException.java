package com.bidcast.selection_service.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Lanzada cuando ocurre un error al intentar liquidar una oferta específica
 * durante el cierre de la sesión.
 */
public class OfferSettlementException extends SelectionDomainException {
    public OfferSettlementException(String offerId, String reason) {
        super(String.format("Error settling offer %s: %s", offerId, reason),
              HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
