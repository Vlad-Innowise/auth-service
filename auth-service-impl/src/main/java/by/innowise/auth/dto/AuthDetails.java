package by.innowise.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthDetails(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password
) {
}
