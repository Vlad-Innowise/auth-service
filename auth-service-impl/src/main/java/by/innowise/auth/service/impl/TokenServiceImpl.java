package by.innowise.auth.service.impl;

import by.innowise.auth.dto.token.TokenRequestDto;
import by.innowise.auth.dto.token.TokenResponseDto;
import by.innowise.auth.exception.TokenValidationException;
import by.innowise.auth.mapper.RefreshTokenMapper;
import by.innowise.auth.repository.TokenRepository;
import by.innowise.auth.repository.entity.AuthUser;
import by.innowise.auth.repository.entity.RefreshToken;
import by.innowise.auth.service.RefreshTokenCleanupService;
import by.innowise.auth.service.TokenService;
import by.innowise.auth.service.dto.ClaimsDto;
import by.innowise.auth.service.dto.ParsedTokenDto;
import by.innowise.auth.service.dto.RefreshTokenCreateDto;
import by.innowise.auth.service.dto.TokenType;
import by.innowise.auth.util.DateTimeUtil;
import by.innowise.auth.util.TokenHasher;
import by.innowise.internship.security.config.JwtSecurityProperties;
import by.innowise.internship.security.dto.Role;
import by.innowise.internship.security.util.JwtConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenServiceImpl implements TokenService, RefreshTokenCleanupService {

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

    @Override
    public void validate(TokenRequestDto token) {
        try {
            jwtParser.parseSignedClaims(token.token());
        } catch (JwtException | IllegalArgumentException e) {
            throw new TokenValidationException("The provided token is invalid or expired", HttpStatus.UNAUTHORIZED, e);
        }
    }

    @Override
    public ParsedTokenDto getParsedTokenClaims(TokenRequestDto tokenRequest) {
        log.info("Get token payload");
        Claims claims = jwtParser.parseSignedClaims(tokenRequest.token())
                                 .getPayload();
        log.info("Parsing token claims");
        return generateParsedTokenDtoFromClaims(claims);
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<RefreshToken> getRefreshTokenByTokenHash(String hashedToken) {
        return findRefreshTokenByTokenHash(hashedToken);
    }

    @Override
    public void delete(RefreshToken token) {
        log.info("Requested to delete a token: {}", token.getId());
        tokenRepository.delete(token);
    }

    @Override
    public Optional<RefreshToken> getRefreshTokenByUserId(Long userId) {
        return getTokenByUserId(userId);
    }

    @Transactional
    @Override
    public void deleteForUser(Long userId) {
        log.info("Removing refresh token for user: {}", userId);
        getTokenByUserId(userId).ifPresentOrElse(token -> {
                                                     tokenRepository.delete(token);
                                                     log.info("Refresh token for user: {} pre-deleted", userId);
                                                 },
                                                 () -> log.info("Not found refresh tokens for user: {}", userId));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void clearTokenIfStored(String hashedToken) {
        log.info("Requested to delete a token by hash in a separate transaction");
        findRefreshTokenByTokenHash(hashedToken)
                .ifPresentOrElse(t -> {
                                     log.info("Refresh token found: {}", t);
                                     tokenRepository.delete(t);
                                     log.info("Refresh token was deleted successfully: {}", t.getId());
                                 },
                                 () -> log.info("No refresh token found for hash: {}", hashedToken));
    }

    private Optional<RefreshToken> getTokenByUserId(Long userId) {
        log.info("Retrieving refresh token userId: {}", userId);
        return tokenRepository.findTokenByAuthUserId(userId);
    }

    private Optional<RefreshToken> findRefreshTokenByTokenHash(String hashedToken) {
        log.info("Retrieving refresh token by token hash in HEX: {}", hashedToken);
        return tokenRepository.findTokenByTokenHash(hashedToken);
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

    private ParsedTokenDto generateParsedTokenDtoFromClaims(Claims claims) {
        return ParsedTokenDto.builder()
                             .userId(Long.parseLong(claims.getSubject()))
                             .issuedAt(DateTimeUtil.convertDateToLocalDateTime(claims.getIssuedAt()))
                             .expiresAt(DateTimeUtil.convertDateToLocalDateTime(claims.getExpiration()))
                             .tokenType(TokenType.fromType(
                                     claims.get(JwtConstants.JWT_TOKEN_TYPE_CLAIM_NAME, String.class)))
                             .email(claims.get(JwtConstants.JWT_EMAIL_CLAIM_NAME, String.class))
                             .role(getRoleFromClaims(claims))
                             .build();
    }

    private Role getRoleFromClaims(Claims claims) {
        List<?> rawRoles = claims.get(JwtConstants.JWT_ROLE_CLAIM_NAME, List.class);
        List<Role> roles = rawRoles.stream()
                                   .filter(String.class::isInstance)
                                   .map(String.class::cast)
                                   .map(this::getRoleFromString)
                                   .toList();
        if (roles.size() != 1) {
            throw new UnsupportedOperationException(
                    "%s claims cannot be empty or more than 2".formatted(JwtConstants.JWT_ROLE_CLAIM_NAME));
        }
        return roles.getFirst();

    }

    private Role getRoleFromString(String roleWithPrefix) {
        return Role.valueOf(roleWithPrefix.substring(SECURITY_ROLE_PREFIX.length()));
    }


}
