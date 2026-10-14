package info.prorabka.abik.server.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Запрос на logout.
 */
public record LogoutRequest(
        @NotBlank(message = "Refresh-токен обязателен")
        String refreshToken
) {}
