package by.innowise.auth.service.facade;

import by.innowise.auth.dto.UserCreateDto;
import by.innowise.auth.dto.token.TokenRequestDto;
import by.innowise.auth.dto.token.TokenResponseDto;
import by.innowise.auth.exception.TokenValidationException;
import by.innowise.auth.repository.entity.AuthUser;
import by.innowise.auth.repository.entity.RefreshToken;
import by.innowise.auth.service.RefreshTokenCleanupService;
import by.innowise.auth.service.TokenService;
import by.innowise.auth.service.UserService;
import by.innowise.auth.service.dto.ParsedTokenDto;
import by.innowise.auth.service.dto.TokenType;
import by.innowise.auth.util.TokenHasher;
import by.innowise.common.library.exception.UserNotFoundException;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthFacadeImpl implements AuthFacade {

    private final UserService userService;
    private final TokenService tokenService;
    private final RefreshTokenCleanupService refreshTokenCleanupService;

    @Transactional
    @Override
    public TokenResponseDto register(UserCreateDto userCreateDto) {
        log.info("Invoking authService to create a new user from dto: {}", userCreateDto);
        AuthUser user = userService.create(userCreateDto);
        log.info("Invoking token service to generate a token for a user: {}", user);
        return tokenService.generate(user);
    }

    @Transactional
    @Override
    public void validate(TokenRequestDto tokenRequest) {
        validateJwt(tokenRequest);
        ParsedTokenDto parsedTokenDto = parseToken(tokenRequest);
        validateTokenConsistency(tokenRequest, parsedTokenDto);
    }

    @Transactional
    @Override
    public TokenResponseDto refresh(TokenRequestDto tokenRequest) {
        validateJwt(tokenRequest);
        ParsedTokenDto parsedTokenDto = parseToken(tokenRequest);
        checkIfRefreshToken(parsedTokenDto);
        validateTokenConsistency(tokenRequest, parsedTokenDto);
        return generateUpdatedTokens(tokenRequest, parsedTokenDto);
    }

    private void checkIfRefreshToken(ParsedTokenDto parsedTokenDto) {
        if (isNotRefreshToken(parsedTokenDto)) {
            throw new TokenValidationException(
                    "Token type [%s] can't be refreshed, cause it's not a REFRESH token".formatted(
                            parsedTokenDto.getTokenType().getType()), HttpStatus.CONFLICT);
        }
    }

    private TokenResponseDto generateUpdatedTokens(TokenRequestDto tokenRequest, ParsedTokenDto parsedTokenDto) {
        return userService.getActiveById(parsedTokenDto.getUserId())
                          .map(u -> getRefreshedTokens(tokenRequest, u))
                          .orElseThrow(() -> new IllegalStateException(
                                  "User has been illegally removed during token refresh"));
    }

    private TokenResponseDto getRefreshedTokens(TokenRequestDto tokenRequest, AuthUser user) {
        return tokenService.getRefreshTokenByTokenHash(convertTokenToHex(tokenRequest))
                           .map(t -> clearAndGenerateNewTokens(user, t))
                           .orElseGet(() -> tokenService.generate(user));
    }

    private TokenResponseDto clearAndGenerateNewTokens(AuthUser user, RefreshToken token) {
        log.info("Refresh token found: {}", token);
        tokenService.delete(token);
        log.info("Refresh token was pre-deleted: {}", token.getId());
        return tokenService.generate(user);
    }

    private void validateJwt(TokenRequestDto tokenRequest) {
        log.info("Validating if token is not expired or malformed: {}", tokenRequest.token());
        tokenService.validate(tokenRequest);
    }

    private ParsedTokenDto parseToken(TokenRequestDto tokenRequest) {
        ParsedTokenDto parsedTokenDto = tokenService.getParsedTokenClaims(tokenRequest);
        log.info("Retrieved parsed token dto from claims: {}", parsedTokenDto);
        return parsedTokenDto;
    }

    private void validateTokenConsistency(TokenRequestDto tokenRequest, ParsedTokenDto parsedTokenDto) {
        userService.getActiveById(parsedTokenDto.getUserId())
                   .ifPresentOrElse(u -> validateClaimsOrClearRefreshTokenAndThrow(u, parsedTokenDto, tokenRequest),
                                    () -> clearRefreshTokenAndThrow(tokenRequest, parsedTokenDto));
    }

    private void clearRefreshTokenAndThrow(TokenRequestDto tokenRequest, ParsedTokenDto parsedTokenDto) {
        log.info("Auth user with id: [{}] is not found", parsedTokenDto.getUserId());
        clearRefreshTokenIfStored(tokenRequest, parsedTokenDto);
        throw new UserNotFoundException("The subject user is not found or deactivated", HttpStatus.UNAUTHORIZED);
    }

    private void validateClaimsOrClearRefreshTokenAndThrow(@NotNull AuthUser user,
                                                           ParsedTokenDto parsedTokenDto,
                                                           TokenRequestDto tokenRequest) {
        log.info("Retrieved auth user: {}", user);
        log.info("Checking if token claims are consistent with user state in db");
        if (tokenClaimsIsNotConsistent(user, parsedTokenDto)) {
            log.info("Token claims and user state are not consistent! User: {}, parsedClaims: {}", user,
                     parsedTokenDto);
            clearRefreshTokenIfStored(tokenRequest, parsedTokenDto);
            throw new TokenValidationException("Token contains insufficient data", HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean tokenClaimsIsNotConsistent(AuthUser user, ParsedTokenDto parsedTokenDto) {
        return user.getRole() != parsedTokenDto.getRole()
                || !user.getEmail().equalsIgnoreCase(parsedTokenDto.getEmail());
    }

    private void clearRefreshTokenIfStored(TokenRequestDto tokenRequest, ParsedTokenDto parsedTokenDto) {
        log.info("Token is a refresh token: {}", isRefreshToken(parsedTokenDto));
        if (isRefreshToken(parsedTokenDto)) {
            log.info("Checking if refresh token stored in db");
            refreshTokenCleanupService.clearTokenIfStored(convertTokenToHex(tokenRequest));
            log.info("Refresh token was deleted successfully");
        }
    }

    private String convertTokenToHex(TokenRequestDto tokenRequest) {
        return TokenHasher.hashSha256(tokenRequest.token());
    }

    private boolean isRefreshToken(ParsedTokenDto parsedTokenDto) {
        return parsedTokenDto.getTokenType() == TokenType.REFRESH;
    }

    private boolean isNotRefreshToken(ParsedTokenDto parsedTokenDto) {
        return parsedTokenDto.getTokenType() != TokenType.REFRESH;
    }
}
