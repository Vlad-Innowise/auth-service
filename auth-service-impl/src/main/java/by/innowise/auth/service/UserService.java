package by.innowise.auth.service;

import by.innowise.auth.dto.UserCreateDto;
import by.innowise.auth.repository.entity.AuthUser;

import java.util.Optional;

public interface UserService {

    boolean isEmailFree(String email);

    AuthUser create(UserCreateDto userCreateDto);

    Optional<AuthUser> getActiveById(Long userId);
}
