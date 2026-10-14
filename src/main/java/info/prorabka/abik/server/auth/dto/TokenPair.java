package info.prorabka.abik.server.auth.dto;

import java.time.OffsetDateTime;

/**
 * Пара токенов, которую получает клиент при логине/refresh.
 */
public record TokenPair(
        String accessToken,
        String refreshToken,
        OffsetDateTime accessExpiresAt,
        OffsetDateTime refreshExpiresAt
) {}
