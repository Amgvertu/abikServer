package info.prorabka.abik.server.common;

import info.prorabka.abik.server.common.exception.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/v1")
@Tag(name = "Health", description = "Проверка работоспособности сервиса. Доступно без авторизации.")
public class HealthController {

    @Operation(
            summary = "Проверка работоспособности",
            description = "Возвращает статус сервиса и текущее время сервера. Используется для мониторинга."
    )
    @ApiResponse(responseCode = "200", description = "Сервис работает")
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "service", "abik-server",
                "timestamp", OffsetDateTime.now()
        );
    }

    @Operation(
            summary = "Тест обработчика ошибок",
            description = "Всегда бросает 404. Используется для проверки единого формата ошибок."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Никогда — этот эндпоинт всегда падает"),
            @ApiResponse(responseCode = "404", description = "NOT_FOUND — тестовая ошибка")
    })
    @GetMapping("/health/throw-not-found")
    public Map<String, Object> testError() {
        throw new NotFoundException("Тестовый объект не найден");
    }
}
