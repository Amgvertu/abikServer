package info.prorabka.abik.server.auth.dto;

/**
 * Ответ на старт регистрации.
 */
public record RegisterStartResponse(
        String phone,
        String message
) {}