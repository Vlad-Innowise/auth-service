package by.innowise.auth.controller;

import by.innowise.auth.dto.AuthDetails;
import by.innowise.auth.dto.UserCreateDto;
import by.innowise.auth.dto.token.TokenResponseDto;
import by.innowise.auth.service.facade.AuthFacade;
import by.innowise.internship.security.dto.UserHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private final AuthFacade authFacade;

    @PostMapping("/register")
    public ResponseEntity<TokenResponseDto> register(@RequestBody @Valid UserCreateDto userCreateDto) {
        log.info("Requested to create a user: {}", userCreateDto.email());
        TokenResponseDto generatedTokens = authFacade.register(userCreateDto);
        log.info("User created and tokens are generated. Sending a token response to a client: {}", generatedTokens);
        return ResponseEntity.ok(generatedTokens);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> authenticate(@RequestBody @Valid AuthDetails authDetails) {
        log.info("Requested to authenticate user: {}", authDetails.email());
        TokenResponseDto generatedTokens = authFacade.login(authDetails);
        log.info("User authenticated. Tokens successfully generated: {}", generatedTokens);
        return ResponseEntity.ok(generatedTokens);
    }

    @DeleteMapping("/remove")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserHolder userHolder) {
        Long userId = userHolder.crossServiceUserId();
        log.info("Requested to delete a user: {}", userId);
        authFacade.delete(userId);
        log.info("User {} deleted successfully", userId);
        return ResponseEntity.ok()
                             .build();
    }

}
