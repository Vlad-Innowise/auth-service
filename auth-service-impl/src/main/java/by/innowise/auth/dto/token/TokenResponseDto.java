package by.innowise.auth.dto.token;

public record TokenResponseDto(
        String accessToken,
        String refreshToken
) {
}
