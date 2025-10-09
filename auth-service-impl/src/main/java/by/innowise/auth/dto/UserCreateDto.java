package by.innowise.auth.dto;

import by.innowise.auth.validation.api.EmailAvailable;
import by.innowise.common.library.validation.EnumValidator;
import by.innowise.internship.security.dto.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateDto(

        @NotBlank(message = "User email address can't be null")
        @Email(message = "Invalid email address")
        @Size(max = 255, message = "User email can't exceed 255 symbols")
        @EmailAvailable
        String email,

        @NotBlank(message = "User password can't be blank")
        @Size(min = 8, max = 64, message = "User password should be min 8 symbols and not exceed 64 symbols")
        String password,

        @NotBlank(message = "User role can't be blank")
        @EnumValidator(enumClass = Role.class)
        String role
) {
}
