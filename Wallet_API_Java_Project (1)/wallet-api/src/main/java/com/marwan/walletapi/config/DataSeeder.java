package com.marwan.walletapi.config;

import com.marwan.walletapi.transaction.Transaction;
import com.marwan.walletapi.transaction.TransactionRepository;
import com.marwan.walletapi.transaction.TransactionType;
import com.marwan.walletapi.user.Role;
import com.marwan.walletapi.user.User;
import com.marwan.walletapi.user.UserRepository;
import com.marwan.walletapi.wallet.Wallet;
import com.marwan.walletapi.wallet.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Seeds two demo accounts on first startup so the API is immediately explorable
 * via Swagger UI. Disabled automatically once any user exists.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, WalletRepository walletRepository,
                      TransactionRepository transactionRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        User admin = createUser("Admin User", "admin@walletapi.io", "Admin@1234", Role.ADMIN);
        createWallet(admin, BigDecimal.ZERO, null);

        User demo = createUser("Sara Demo", "sara@walletapi.io", "User@1234", Role.USER);
        createWallet(demo, new BigDecimal("500.00"), TransactionType.DEPOSIT);

        log.info("Seeded demo accounts: admin@walletapi.io / Admin@1234 (admin) and "
                + "sara@walletapi.io / User@1234 (wallet with 500.00)");
    }

    private User createUser(String fullName, String email, String rawPassword, Role role) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        return userRepository.save(user);
    }

    private void createWallet(User owner, BigDecimal balance, TransactionType initialTxType) {
        Wallet wallet = new Wallet();
        wallet.setOwner(owner);
        wallet.setBalance(balance);
        walletRepository.save(wallet);

        if (initialTxType != null) {
            Transaction tx = new Transaction();
            tx.setWallet(wallet);
            tx.setType(initialTxType);
            tx.setAmount(balance);
            tx.setBalanceAfter(balance);
            transactionRepository.save(tx);
        }
    }
}
