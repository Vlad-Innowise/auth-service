package by.innowise.auth.service.dto;

import by.innowise.auth.repository.entity.AuthUser;

import java.time.LocalDateTime;
import java.util.UUID;

public record RefreshTokenCreateDto(
        UUID id,
        String tokenHash,
        LocalDateTime expiresAt,
        AuthUser authUser
) {
}
