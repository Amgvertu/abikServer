package info.prorabka.abik.server.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Нет прав на действие (HTTP 403).
 */
public class ForbiddenException extends ApiException {
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }
}