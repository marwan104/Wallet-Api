package com.marwan.walletapi.wallet.dto;

import com.marwan.walletapi.wallet.Wallet;

import java.math.BigDecimal;
import java.time.Instant;

public record WalletResponse(
        Long id,
        String ownerEmail,
        String ownerName,
        BigDecimal balance,
        Instant createdAt) {

    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(wallet.getId(), wallet.getOwner().getEmail(),
                wallet.getOwner().getFullName(), wallet.getBalance(), wallet.getCreatedAt());
    }
}
