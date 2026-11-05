package info.prorabka.abik.server.common.exception;

import org.springframework.http.HttpStatus;

/** Локальная версия события устарела: автоматически перезаписывать сервер нельзя. */
public class OfflineConflictException extends ApiException {
    public OfflineConflictException(String message) {
        super(HttpStatus.CONFLICT, "OFFLINE_CONFLICT", message);
    }
}
