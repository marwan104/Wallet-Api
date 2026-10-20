package com.marwan.walletapi.user.dto;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInSeconds,
        String fullName,
        String email,
        String role) {
}
