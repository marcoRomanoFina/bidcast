package com.bidcast.auction_service.client;

import java.math.BigDecimal;

// dto para el wallet client
public record WalletFreezeRequest(
    String advertiserId,
    BigDecimal amount,
    String referenceId,
    String reason
) {}
