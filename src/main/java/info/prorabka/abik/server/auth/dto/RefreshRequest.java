package info.prorabka.abik.server.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Запрос на обновление пары токенов.
 */
public record RefreshRequest(
        @NotBlank(message = "Refresh-токен обязателен")
        String refreshToken
) {}