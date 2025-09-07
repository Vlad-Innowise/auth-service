package by.innowise.auth.dto.token;

import jakarta.validation.constraints.NotBlank;

public record TokenRequestDto(
        @NotBlank String token
) {
}
