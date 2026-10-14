package info.prorabka.abik.server.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Пользователь не аутентифицирован (HTTP 401).
 */
public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }
}
