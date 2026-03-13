package com.bidcast.auction_service.auction;

import lombok.Builder;
import java.util.UUID;

@Builder
public record WinningAd(
    UUID bidId,
    String mediaUrl,
    String advertiserId,
    String campaignId,
    String playReceiptId
) implements AuctionResult {}
