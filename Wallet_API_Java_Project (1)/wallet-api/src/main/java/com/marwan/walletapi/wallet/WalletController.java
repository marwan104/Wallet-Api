package com.marwan.walletapi.wallet;

import com.marwan.walletapi.transaction.dto.TransactionResponse;
import com.marwan.walletapi.wallet.dto.AmountRequest;
import com.marwan.walletapi.wallet.dto.TransferRequest;
import com.marwan.walletapi.wallet.dto.WalletResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wallets")
@Tag(name = "Wallet", description = "Wallet balance and money movement endpoints")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's wallet")
    public WalletResponse me(Authentication authentication) {
        return walletService.getMyWallet(authentication.getName());
    }

    @PostMapping("/deposit")
    @Operation(summary = "Deposit money into the authenticated user's wallet")
    public WalletResponse deposit(@Valid @RequestBody AmountRequest request, Authentication authentication) {
        return walletService.deposit(authentication.getName(), request);
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw money from the authenticated user's wallet")
    public WalletResponse withdraw(@Valid @RequestBody AmountRequest request, Authentication authentication) {
        return walletService.withdraw(authentication.getName(), request);
    }

    @PostMapping("/transfer")
    @Operation(summary = "Atomically transfer money to another wallet by email")
    public WalletResponse transfer(@Valid @RequestBody TransferRequest request, Authentication authentication) {
        return walletService.transfer(authentication.getName(), request);
    }

    @GetMapping("/transactions")
    @Operation(summary = "Paginated transaction history for the authenticated user's wallet")
    public Page<TransactionResponse> transactions(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        return walletService.getTransactions(authentication.getName(), pageable);
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all wallets (admin only)")
    public List<WalletResponse> allWallets() {
        return walletService.listAllWallets();
    }
}
