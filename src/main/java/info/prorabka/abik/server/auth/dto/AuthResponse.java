package info.prorabka.abik.server.auth.dto;

import info.prorabka.abik.server.user.dto.UserResponse;

/**
 * Ответ на успешный логин или подтверждение регистрации:
 * пара токенов + данные пользователя.
 */
public record AuthResponse(
        TokenPair tokens,
        UserResponse user
) {}