package by.innowise.auth.service.facade;

import by.innowise.auth.dto.AuthDetails;
import by.innowise.auth.dto.UserCreateDto;
import by.innowise.auth.dto.token.TokenRequestDto;
import by.innowise.auth.dto.token.TokenResponseDto;

public interface AuthFacade {

    TokenResponseDto register(UserCreateDto userCreateDto);

    void validate(TokenRequestDto tokenRequest);

    TokenResponseDto refresh(TokenRequestDto tokenRequest);

    TokenResponseDto login(AuthDetails authDetails);

    void delete(Long userId);
}
