package info.prorabka.abik.server.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Единый формат ответа об ошибке.
 * Используется во всех endpoint'ах API.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldError> errors
) {
    /**
     * Ошибка валидации конкретного поля.
     */
    public record FieldError(String field, String message) {}

    /** Простая ошибка без детализации по полям. */
    public static ApiError of(int status, String code, String message, String path) {
        return new ApiError(OffsetDateTime.now(), status, code, message, path, null);
    }

    /** Ошибка с детализацией по полям (обычно для @Valid). */
    public static ApiError of(int status, String code, String message, String path,
                              List<FieldError> errors) {
        return new ApiError(OffsetDateTime.now(), status, code, message, path, errors);
    }
}