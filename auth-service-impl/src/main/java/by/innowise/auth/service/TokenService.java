package by.innowise.auth.service;

import by.innowise.auth.dto.token.TokenRequestDto;
import by.innowise.auth.dto.token.TokenResponseDto;
import by.innowise.auth.repository.entity.AuthUser;
import by.innowise.auth.repository.entity.RefreshToken;
import by.innowise.auth.service.dto.ParsedTokenDto;

import java.util.Optional;

public interface TokenService {

    TokenResponseDto generate(AuthUser user);

    void validate(TokenRequestDto token);

    ParsedTokenDto getParsedTokenClaims(TokenRequestDto tokenRequest);

    Optional<RefreshToken> getRefreshTokenByTokenHash(String hashedToken);

}