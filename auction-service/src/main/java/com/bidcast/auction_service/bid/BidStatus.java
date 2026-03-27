package com.bidcast.auction_service.bid;

/**
 * enum para los status
 */
public enum BidStatus {
    PENDING_RESERVATION,
    ACTIVE,
    CLOSED,
    EXHAUSTED,
    FAILED,
    FAILED_CRITICAL
}
