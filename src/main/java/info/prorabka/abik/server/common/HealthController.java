package info.prorabka.abik.server.common;

import info.prorabka.abik.server.common.exception.NotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Служебный контроллер для проверки работоспособности сервиса.
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    /** Проверка, что сервер отвечает. */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "service", "abik-server",
                "timestamp", OffsetDateTime.now()
        );
    }

    /** Тестовый endpoint — бросает NotFoundException, чтобы проверить обработчик ошибок. */
    @GetMapping("/health/throw-not-found")
    public Map<String, Object> testError() {
        throw new NotFoundException("Тестовый объект не найден");
    }
}
