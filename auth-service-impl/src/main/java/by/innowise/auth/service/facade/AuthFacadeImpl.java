package by.innowise.auth.service.facade;

import by.innowise.auth.dto.token.TokenResponseDto;
import by.innowise.auth.dto.UserCreateDto;
import by.innowise.auth.repository.entity.AuthUser;
import by.innowise.auth.service.TokenService;
import by.innowise.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthFacadeImpl implements AuthFacade {

    private final UserService userService;
    private final TokenService tokenService;

    @Transactional
    @Override
    public TokenResponseDto register(UserCreateDto userCreateDto) {
        log.info("Invoking authService to create a new user from dto: {}", userCreateDto);
        AuthUser user = userService.create(userCreateDto);
        log.info("Invoking token service to generate a token for a user: {}", user);
        return tokenService.generate(user);
    }
}
