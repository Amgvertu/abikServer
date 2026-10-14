package info.prorabka.abik.server.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Базовое исключение приложения.
 * Все бизнес-исключения наследуются от него,
 * чтобы глобальный обработчик мог возвращать согласованный JSON.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}