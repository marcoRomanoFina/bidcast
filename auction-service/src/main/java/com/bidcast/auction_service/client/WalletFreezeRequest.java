package com.bidcast.auction_service.client;

import java.math.BigDecimal;

public record WalletFreezeRequest(
    String advertiserId,
    BigDecimal amount,
    String referenceId,
    String reason
) {}
