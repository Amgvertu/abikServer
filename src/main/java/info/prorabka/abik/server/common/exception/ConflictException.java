package info.prorabka.abik.server.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Конфликт состояния (HTTP 409) — например, дублирование данных.
 */
public class ConflictException extends ApiException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "CONFLICT", message);
    }
}
