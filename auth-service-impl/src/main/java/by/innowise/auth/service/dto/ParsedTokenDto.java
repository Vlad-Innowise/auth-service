package by.innowise.auth.service.dto;

import by.innowise.internship.security.dto.Role;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class ParsedTokenDto {

    TokenType tokenType;

    Long userId;

    LocalDateTime issuedAt;

    LocalDateTime expiresAt;

    String email;

    Role role;
}
