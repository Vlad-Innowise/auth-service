package by.innowise.auth.service.impl;

import by.innowise.auth.dto.UserCreateDto;
import by.innowise.auth.mapper.UserMapper;
import by.innowise.auth.repository.UserRepository;
import by.innowise.auth.repository.entity.AuthUser;
import by.innowise.auth.repository.entity.UserStatus;
import by.innowise.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public boolean isEmailFree(String email) {
        log.info("Checking whether the email [{}] is free", email);
        return userRepository.findByEmailAndStatus(email, UserStatus.ACTIVATED)
                             .isEmpty();
    }

    @Transactional
    @Override
    public AuthUser create(UserCreateDto userCreateDto) {
        AuthUser toSave = mapper.toEntity(userCreateDto,
                                          UserStatus.ACTIVATED,
                                          passwordEncoder.encode(userCreateDto.password())
        );
        log.info("Invoking user repository to save a user: {}", toSave);
        return userRepository.saveAndFlush(toSave);
    }

    @Override
    public Optional<AuthUser> getActiveById(Long userId) {
        log.info("Trying to retrieve active user by id: {}", userId);
        return userRepository.findByIdAndStatus(userId, UserStatus.ACTIVATED);
    }
}
