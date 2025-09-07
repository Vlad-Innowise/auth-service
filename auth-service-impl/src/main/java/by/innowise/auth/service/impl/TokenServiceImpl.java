package by.innowise.auth.service.impl;

import by.innowise.auth.dto.token.TokenResponseDto;
import by.innowise.auth.mapper.RefreshTokenMapper;
import by.innowise.auth.repository.TokenRepository;
import by.innowise.auth.repository.entity.AuthUser;
import by.innowise.auth.repository.entity.RefreshToken;
import by.innowise.auth.service.TokenService;
import by.innowise.auth.service.dto.ClaimsDto;
import by.innowise.auth.service.dto.RefreshTokenCreateDto;
import by.innowise.auth.service.dto.TokenType;
import by.innowise.auth.util.TokenHasher;
import by.innowise.internship.security.config.JwtSecurityProperties;
import by.innowise.internship.security.util.JwtConstants;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenServiceImpl implements TokenService {

    private static final String SECURITY_ROLE_PREFIX = "ROLE_";
    private final JwtParser jwtParser;
    private final JwtSecurityProperties jwtSecurityProperties;
    private final SecretKey secretKey;
    private final RefreshTokenMapper mapper;
    private final TokenRepository tokenRepository;

    @Transactional
    @Override
    public TokenResponseDto generate(AuthUser user) {
        log.info("Generating access and refresh tokens");
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        String accessToken = generateToken(user, now, TokenType.ACCESS);
        String refreshToken = generateToken(user, now, TokenType.REFRESH);
        log.debug("Access: {} and refresh: {} tokens were generated", accessToken, refreshToken);
        RefreshToken toSave = mapper.toEntity(
                getRefreshTokenCreateDto(user,
                                         TokenHasher.hashSha256(refreshToken),
                                         getExpirationDateByTokenType(now, TokenType.REFRESH))
        );
        log.info("Saving refresh token to db: {}", toSave);
        tokenRepository.saveAndFlush(toSave);
        return new TokenResponseDto(accessToken, refreshToken);
    }

    private String generateToken(AuthUser user, LocalDateTime now, TokenType type) {
        ClaimsDto claimsDto = prepareAndGetClaimsForToken(user, now, type);
        return generateAndSign(claimsDto);
    }

    private ClaimsDto prepareAndGetClaimsForToken(AuthUser user, LocalDateTime now, TokenType type) {
        Map<String, Object> customClaims =
                Map.of(JwtConstants.JWT_EMAIL_CLAIM_NAME, user.getEmail(),
                       JwtConstants.JWT_ROLE_CLAIM_NAME, List.of(SECURITY_ROLE_PREFIX + user.getRole()),
                       JwtConstants.JWT_TOKEN_TYPE_CLAIM_NAME, type.getType());
        Instant expiresAt = getExpirationDateByTokenType(now, type).toInstant(ZoneOffset.UTC);
        return ClaimsDto.builder()
                        .subject(user.getId().toString())
                        .issuedAt(Date.from(now.toInstant(ZoneOffset.UTC)))
                        .expiresAt(Date.from(expiresAt))
                        .customClaims(customClaims)
                        .build();
    }

    private LocalDateTime getExpirationDateByTokenType(LocalDateTime now, TokenType type) {
        return now.plus(jwtSecurityProperties.getTtlForType(type.getType()));

    }

    private String generateAndSign(ClaimsDto claimsDto) {
        return Jwts.builder()
                   .issuer(jwtSecurityProperties.getIssuer())
                   .subject(claimsDto.getSubject())
                   .issuedAt(claimsDto.getIssuedAt())
                   .expiration(claimsDto.getExpiresAt())
                   .claims(claimsDto.getCustomClaims())
                   .signWith(secretKey, Jwts.SIG.HS256)
                   .compact();
    }

    private RefreshTokenCreateDto getRefreshTokenCreateDto(AuthUser user, String hashedRefreshToken,
                                                           LocalDateTime expiresAt) {
        return new RefreshTokenCreateDto(UUID.randomUUID(),
                                         hashedRefreshToken,
                                         expiresAt,
                                         user);
    }
}
