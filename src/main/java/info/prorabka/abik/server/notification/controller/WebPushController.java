package info.prorabka.abik.server.notification.controller;

import info.prorabka.abik.server.common.security.SecurityUtils;
import info.prorabka.abik.server.notification.dto.WebPushStatusResponse;
import info.prorabka.abik.server.notification.dto.WebPushSubscribeRequest;
import info.prorabka.abik.server.notification.service.WebPushService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/web-push")
@Tag(name = "Web Push", description = "Подписки браузеров и установленной PWA")
@SecurityRequirement(name = "bearerAuth")
public class WebPushController {

    private final WebPushService service;

    public WebPushController(WebPushService service) {
        this.service = service;
    }

    @Operation(summary = "Состояние Web Push и публичный VAPID-ключ")
    @GetMapping("/public-key")
    public ResponseEntity<WebPushStatusResponse> status() {
        return ResponseEntity.ok(service.status());
    }

    @Operation(summary = "Зарегистрировать или перепривязать браузерную подписку")
    @PostMapping("/subscriptions")
    public ResponseEntity<Map<String, String>> subscribe(
            @Valid @RequestBody WebPushSubscribeRequest request) {
        service.subscribe(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok(Map.of("message", "Браузерные уведомления включены"));
    }

    @Operation(summary = "Отозвать свою браузерную подписку")
    @DeleteMapping("/subscriptions")
    public ResponseEntity<Map<String, String>> unsubscribe(@RequestParam String endpoint) {
        service.unsubscribe(SecurityUtils.getCurrentUserId(), endpoint);
        return ResponseEntity.ok(Map.of("message", "Браузерные уведомления отключены"));
    }
}
