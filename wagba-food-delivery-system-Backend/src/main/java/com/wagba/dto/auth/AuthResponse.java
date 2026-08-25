package com.wagba.dto.auth;

/**
 * Login / Google-login result. `token` is the JWT; the rest lets the frontend
 * render the shell without an extra /auth/me round-trip.
 */
public record AuthResponse(
        String token,
        Long id,
        String name,
        String email,
        String role,
        String status,
        String onboardingStatus
) {}
