package com.marwan.walletapi.transaction.dto;

import com.marwan.walletapi.transaction.Transaction;
import com.marwan.walletapi.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        Long id,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String counterpartyEmail,
        Instant createdAt) {

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(transaction.getId(), transaction.getType(), transaction.getAmount(),
                transaction.getBalanceAfter(), transaction.getCounterpartyEmail(), transaction.getCreatedAt());
    }
}
