package com.marwan.walletapi.common.exception;

public class SameWalletTransferException extends RuntimeException {

    public SameWalletTransferException(String message) {
        super(message);
    }
}
