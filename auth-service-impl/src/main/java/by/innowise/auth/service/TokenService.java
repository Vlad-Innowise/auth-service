package by.innowise.auth.service;

import by.innowise.auth.dto.token.TokenResponseDto;
import by.innowise.auth.repository.entity.AuthUser;

public interface TokenService {

    TokenResponseDto generate(AuthUser user);

}