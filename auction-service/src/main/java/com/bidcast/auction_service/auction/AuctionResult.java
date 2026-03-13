package com.bidcast.auction_service.auction;

/**
 Interfaz sellada para representar el resultado de una subasta.
 */
public sealed interface AuctionResult permits WinningAd, NoAdFound {
}
