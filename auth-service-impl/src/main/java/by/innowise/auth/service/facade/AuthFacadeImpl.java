package by.innowise.auth.service.facade;

import by.innowise.auth.dto.AuthDetails;
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
        ParsedTokenDto parsedTokenDto = validateAndParse(tokenRequest);
        getValidatedUser(tokenRequest, parsedTokenDto);
    }

    @Transactional
    @Override
    public TokenResponseDto refresh(TokenRequestDto tokenRequest) {
        ParsedTokenDto parsedTokenDto = validateAndParse(tokenRequest);
        checkIfRefreshToken(parsedTokenDto);
        AuthUser validatedUser = getValidatedUser(tokenRequest, parsedTokenDto);
        return refreshTokenByTokenHash(validatedUser, convertTokenToHex(tokenRequest));
    }

    @Transactional
    @Override
    public TokenResponseDto login(AuthDetails authDetails) {
        AuthUser authenticated = userService.authenticate(authDetails);
        log.info("Retrieved a user from user service: {}", authenticated);
        return refreshTokenByUser(authenticated);
    }

    @Transactional
    @Override
    public void delete(Long userId) {
        tokenService.deleteForUser(userId);
        userService.delete(userId);
    }

    private ParsedTokenDto validateAndParse(TokenRequestDto tokenRequest) {
        validateJwt(tokenRequest);
        return parseToken(tokenRequest);
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

    private AuthUser getValidatedUser(TokenRequestDto tokenRequest, ParsedTokenDto parsedTokenDto) {
        return userService.getActiveById(parsedTokenDto.getUserId())
                          .map(u -> {
                              ensureClaimsAreConsistent(u, parsedTokenDto, tokenRequest);
                              return u;
                          })
                          .orElseThrow(() -> {
                              clearRefreshToken(tokenRequest, parsedTokenDto);
                              return new TokenValidationException("The subject user is not found or deactivated",
                                                                  HttpStatus.UNAUTHORIZED);
                          });
    }

    private void checkIfRefreshToken(ParsedTokenDto parsedTokenDto) {
        if (isNotRefreshToken(parsedTokenDto)) {
            throw new TokenValidationException(
                    "Token type [%s] can't be refreshed, cause it's not a REFRESH token".formatted(
                            parsedTokenDto.getTokenType().getType()), HttpStatus.CONFLICT);
        }
    }

    private TokenResponseDto refreshTokenByTokenHash(AuthUser user, String hashedToken) {
        return tokenService.getRefreshTokenByTokenHash(hashedToken)
                           .map(t -> replaceRefreshToken(user, t))
                           .orElseGet(() -> tokenService.generate(user));
    }

    private TokenResponseDto refreshTokenByUser(AuthUser user) {
        return tokenService.getRefreshTokenByUserId(user.getId())
                           .map(t -> replaceRefreshToken(user, t))
                           .orElseGet(() -> tokenService.generate(user));
    }

    private TokenResponseDto replaceRefreshToken(AuthUser user, RefreshToken token) {
        log.info("Refresh token found: {}", token);
        tokenService.delete(token);
        log.info("Refresh token was pre-deleted: {}", token.getId());
        return tokenService.generate(user);
    }

    private void ensureClaimsAreConsistent(@NotNull AuthUser user,
                                           ParsedTokenDto parsedTokenDto,
                                           TokenRequestDto tokenRequest) {
        log.info("Retrieved auth user: {}", user);
        log.info("Checking if token claims are consistent with user state in db");
        if (tokenClaimsIsNotConsistent(user, parsedTokenDto)) {
            log.info("Token claims and user state are not consistent! User: {}, parsedClaims: {}", user,
                     parsedTokenDto);
            handleInconsistentClaims(parsedTokenDto, tokenRequest);
        }
    }

    private void handleInconsistentClaims(ParsedTokenDto parsedTokenDto, TokenRequestDto tokenRequest) {
        clearRefreshTokenIfStored(tokenRequest, parsedTokenDto);
        throw new TokenValidationException("Token contains insufficient data", HttpStatus.UNAUTHORIZED);
    }

    private void clearRefreshToken(TokenRequestDto tokenRequest, ParsedTokenDto parsedTokenDto) {
        log.info("Auth user with id: [{}] is not found", parsedTokenDto.getUserId());
        clearRefreshTokenIfStored(tokenRequest, parsedTokenDto);
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
