package info.prorabka.abik.server.auth.controller;

import info.prorabka.abik.server.auth.dto.*;
import info.prorabka.abik.server.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST-контроллер аутентификации.
 * Все endpoint'ы доступны без авторизации (см. SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Шаг 1 регистрации: создаём пользователя (PENDING) и отправляем SMS-код. */
    @PostMapping("/register/start")
    public ResponseEntity<RegisterStartResponse> registerStart(
            @Valid @RequestBody RegisterStartRequest req,
            HttpServletRequest httpRequest) {
        String ip = extractClientIp(httpRequest);
        RegisterStartResponse response = authService.registerStart(req, ip);
        return ResponseEntity.ok(response);
    }

    /** Шаг 2 регистрации: подтверждаем код, получаем пару токенов. */
    @PostMapping("/register/confirm")
    public ResponseEntity<AuthResponse> registerConfirm(
            @Valid @RequestBody RegisterConfirmRequest req) {
        return ResponseEntity.ok(authService.registerConfirm(req));
    }

    /** Логин по телефону и паролю. */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    /** Обновление пары токенов по refresh. */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        return ResponseEntity.ok(authService.refresh(req));
    }

    /** Logout — отзыв refresh-токена. */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@Valid @RequestBody LogoutRequest req) {
        authService.logout(req);
        return ResponseEntity.ok(Map.of("message", "Вы вышли из системы"));
    }

    // ============================================================
    // Helpers
    // ============================================================

    /** Определяем IP клиента, учитывая прокси-заголовки (nginx). */
    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // X-Forwarded-For может содержать список IP — берём первый
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}