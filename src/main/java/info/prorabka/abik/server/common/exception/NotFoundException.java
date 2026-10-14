package info.prorabka.abik.server.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Ресурс не найден (HTTP 404).
 */
public class NotFoundException extends ApiException {
    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }
}