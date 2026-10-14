package info.prorabka.abik.server.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Некорректный запрос (HTTP 400).
 */
public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }
}