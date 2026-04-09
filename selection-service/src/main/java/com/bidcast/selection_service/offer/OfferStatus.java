package com.bidcast.selection_service.offer;

/**
 * Estados de lifecycle de una SessionOffer.
 *
 * No describen solo "vida o muerte" de la offer, sino en qué parte del flujo
 * operativo/financiero se encuentra.
 */
public enum OfferStatus {
    PENDING_RESERVATION,
    ACTIVE,
    CLOSED,
    EXHAUSTED,
    FAILED,
    FAILED_CRITICAL
}
