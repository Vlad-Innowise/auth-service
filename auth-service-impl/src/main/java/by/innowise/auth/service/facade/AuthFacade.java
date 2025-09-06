package by.innowise.auth.service.facade;

import by.innowise.auth.dto.token.TokenResponseDto;
import by.innowise.auth.dto.UserCreateDto;

public interface AuthFacade {

    TokenResponseDto register(UserCreateDto userCreateDto);

}
