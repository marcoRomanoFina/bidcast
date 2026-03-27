package com.bidcast.wallet_service.core.exception;

public class PlatformWalletNotConfiguredException extends RuntimeException {

    public PlatformWalletNotConfiguredException() {
        super("Fatal configuration: platform wallet not found");
    }
}
