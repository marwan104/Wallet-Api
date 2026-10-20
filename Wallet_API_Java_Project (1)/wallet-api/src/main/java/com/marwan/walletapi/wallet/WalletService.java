package com.marwan.walletapi.wallet;

import com.marwan.walletapi.common.exception.InsufficientBalanceException;
import com.marwan.walletapi.common.exception.ResourceNotFoundException;
import com.marwan.walletapi.common.exception.SameWalletTransferException;
import com.marwan.walletapi.transaction.Transaction;
import com.marwan.walletapi.transaction.TransactionRepository;
import com.marwan.walletapi.transaction.TransactionType;
import com.marwan.walletapi.transaction.dto.TransactionResponse;
import com.marwan.walletapi.wallet.dto.AmountRequest;
import com.marwan.walletapi.wallet.dto.TransferRequest;
import com.marwan.walletapi.wallet.dto.WalletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public WalletService(WalletRepository walletRepository, TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public WalletResponse getMyWallet(String email) {
        return WalletResponse.from(getWalletByEmail(email));
    }

    @Transactional(readOnly = true)
    public List<WalletResponse> listAllWallets() {
        return walletRepository.findAll().stream().map(WalletResponse::from).toList();
    }

    @Transactional
    public WalletResponse deposit(String email, AmountRequest request) {
        Wallet wallet = getWalletByEmail(email);
        wallet.credit(request.amount());
        walletRepository.save(wallet);
        recordTransaction(wallet, TransactionType.DEPOSIT, request.amount(), null);
        return WalletResponse.from(wallet);
    }

    @Transactional
    public WalletResponse withdraw(String email, AmountRequest request) {
        Wallet wallet = getWalletByEmail(email);
        ensureSufficientBalance(wallet, request.amount());
        wallet.debit(request.amount());
        walletRepository.save(wallet);
        recordTransaction(wallet, TransactionType.WITHDRAW, request.amount(), null);
        return WalletResponse.from(wallet);
    }

    /**
     * Atomically moves money between two wallets: both sides are updated and both
     * audit records are written inside one database transaction, so a failure can
     * never leave money "in flight".
     */
    @Transactional
    public WalletResponse transfer(String email, TransferRequest request) {
        String targetEmail = request.toEmail().trim().toLowerCase();
        if (targetEmail.equals(email)) {
            throw new SameWalletTransferException("Cannot transfer to your own wallet");
        }

        Wallet source = getWalletByEmail(email);
        Wallet target = walletRepository.findByOwnerEmail(targetEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No wallet found for email: " + targetEmail));
        ensureSufficientBalance(source, request.amount());

        source.debit(request.amount());
        target.credit(request.amount());
        walletRepository.save(source);
        walletRepository.save(target);

        recordTransaction(source, TransactionType.TRANSFER_OUT, request.amount(), targetEmail);
        recordTransaction(target, TransactionType.TRANSFER_IN, request.amount(), email);
        return WalletResponse.from(source);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(String email, Pageable pageable) {
        Wallet wallet = getWalletByEmail(email);
        return transactionRepository.findByWalletIdOrderByCreatedAtDescIdDesc(wallet.getId(), pageable)
                .map(TransactionResponse::from);
    }

    private Wallet getWalletByEmail(String email) {
        return walletRepository.findByOwnerEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for: " + email));
    }

    private void ensureSufficientBalance(Wallet wallet, BigDecimal amount) {
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance: available "
                    + wallet.getBalance() + ", requested " + amount);
        }
    }

    private void recordTransaction(Wallet wallet, TransactionType type, BigDecimal amount,
                                   String counterpartyEmail) {
        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setBalanceAfter(wallet.getBalance());
        transaction.setCounterpartyEmail(counterpartyEmail);
        transactionRepository.save(transaction);
    }
}
