package com.marwan.walletapi.user;

import com.marwan.walletapi.common.exception.DuplicateResourceException;
import com.marwan.walletapi.common.exception.ResourceNotFoundException;
import com.marwan.walletapi.security.JwtService;
import com.marwan.walletapi.user.dto.AuthResponse;
import com.marwan.walletapi.user.dto.LoginRequest;
import com.marwan.walletapi.user.dto.RegisterRequest;
import com.marwan.walletapi.wallet.Wallet;
import com.marwan.walletapi.wallet.WalletRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, WalletRepository walletRepository,
                       PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("An account with email '" + email + "' already exists");
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        userRepository.save(user);

        Wallet wallet = new Wallet();
        wallet.setOwner(user);
        wallet.setBalance(BigDecimal.ZERO);
        walletRepository.save(wallet);

        return buildResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return buildResponse(user);
    }

    private AuthResponse buildResponse(User user) {
        return new AuthResponse(jwtService.generateToken(user), "Bearer",
                jwtService.getExpirationSeconds(), user.getFullName(), user.getEmail(),
                user.getRole().name());
    }
}
