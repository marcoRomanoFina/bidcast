package com.bidcast.wallet_service.transaction;

public enum WalletTransactionType {
    DEPOSIT,
    WITHDRAWAL,
    ADJUSTMENT,
    POP_CHARGE_ADVERTISER_DEBIT,
    POP_PUBLISHER_CREDIT,
    POP_PLATFORM_FEE
}