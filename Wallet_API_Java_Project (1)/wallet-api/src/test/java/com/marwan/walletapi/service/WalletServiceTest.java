package com.marwan.walletapi.service;

import com.marwan.walletapi.common.exception.InsufficientBalanceException;
import com.marwan.walletapi.common.exception.ResourceNotFoundException;
import com.marwan.walletapi.common.exception.SameWalletTransferException;
import com.marwan.walletapi.transaction.Transaction;
import com.marwan.walletapi.transaction.TransactionRepository;
import com.marwan.walletapi.user.User;
import com.marwan.walletapi.wallet.Wallet;
import com.marwan.walletapi.wallet.WalletRepository;
import com.marwan.walletapi.wallet.WalletService;
import com.marwan.walletapi.wallet.dto.AmountRequest;
import com.marwan.walletapi.wallet.dto.TransferRequest;
import com.marwan.walletapi.wallet.dto.WalletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private WalletService walletService;

    private User owner;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setEmail("sara@walletapi.io");
        owner.setFullName("Sara Demo");
        wallet = new Wallet();
        wallet.setOwner(owner);
        wallet.setBalance(new BigDecimal("100.00"));
    }

    @Test
    void deposit_updatesBalanceAndRecordsTransaction() {
        when(walletRepository.findByOwnerEmail(owner.getEmail())).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        WalletResponse response =
                walletService.deposit(owner.getEmail(), new AmountRequest(new BigDecimal("40.50")));

        assertThat(response.balance()).isEqualByComparingTo("140.50");
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void withdraw_insufficientBalance_throwsAndSavesNothing() {
        when(walletRepository.findByOwnerEmail(owner.getEmail())).thenReturn(Optional.of(wallet));

        assertThatThrownBy(() -> walletService.withdraw(owner.getEmail(),
                new AmountRequest(new BigDecimal("500.00"))))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Insufficient balance");

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_movesFundsAndWritesBothAuditRecords() {
        when(walletRepository.findByOwnerEmail("sara@walletapi.io")).thenReturn(Optional.of(wallet));

        User recipient = new User();
        recipient.setEmail("omar@walletapi.io");
        Wallet target = new Wallet();
        target.setOwner(recipient);
        target.setBalance(new BigDecimal("10.00"));
        when(walletRepository.findByOwnerEmail("omar@walletapi.io")).thenReturn(Optional.of(target));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        WalletResponse response = walletService.transfer("sara@walletapi.io",
                new TransferRequest("Omar@Walletapi.io", new BigDecimal("30.00")));

        assertThat(response.balance()).isEqualByComparingTo("70.00");
        assertThat(target.getBalance()).isEqualByComparingTo("40.00");
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void transfer_toSelf_throwsAndSavesNothing() {
        assertThatThrownBy(() -> walletService.transfer("sara@walletapi.io",
                new TransferRequest("SARA@walletapi.io", new BigDecimal("10.00"))))
                .isInstanceOf(SameWalletTransferException.class);

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transfer_toUnknownWallet_throwsNotFound() {
        when(walletRepository.findByOwnerEmail("sara@walletapi.io")).thenReturn(Optional.of(wallet));
        when(walletRepository.findByOwnerEmail("ghost@walletapi.io")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.transfer("sara@walletapi.io",
                new TransferRequest("ghost@walletapi.io", new BigDecimal("10.00"))))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(transactionRepository, never()).save(any());
    }
}
