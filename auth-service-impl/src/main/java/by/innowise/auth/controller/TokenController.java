package by.innowise.auth.controller;

import by.innowise.auth.dto.token.TokenRequestDto;
import by.innowise.auth.service.facade.AuthFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/token")
@Slf4j
@RequiredArgsConstructor
public class TokenController {

    private final AuthFacade authFacade;

    @PostMapping("/validate")
    public ResponseEntity<Void> validate(@RequestBody @Valid TokenRequestDto tokenRequest) {
        log.info("Requested to validate token: {}", tokenRequest.token());
        authFacade.validate(tokenRequest);
        log.info("Token validated successfully");
        return ResponseEntity.ok()
                             .build();
    }

}
