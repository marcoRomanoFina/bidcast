package com.bidcast.billing_service.payment.event;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletCreditEvent(
    UUID advertiserId,
    BigDecimal amount,
    String paymentId,
    String referenceId
) {}
