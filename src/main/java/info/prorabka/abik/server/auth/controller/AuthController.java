package info.prorabka.abik.server.auth.controller;

import info.prorabka.abik.server.auth.dto.*;
import info.prorabka.abik.server.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/auth")
@Tag(name = "Auth",
        description = "Регистрация, вход, обновление токенов. Все эндпоинты доступны без авторизации.")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Шаг 1 регистрации — запросить SMS-код",
            description = """
                    Создаёт пользователя со статусом PENDING и отправляет 4-значный код на указанный номер.

                    **Что делает:**
                    - Проверяет согласие на обработку ПД (обязательно).
                    - Нормализует телефон в формат E.164.
                    - Если пользователь уже есть со статусом ACTIVE — вернёт 409.
                    - Если статус PENDING — обновит пароль и отправит новый код.
                    - Сохраняет согласие в user_consents.

                    **В mock-режиме** код пишется в лог приложения: `[SMS-MOCK] to=..., text="Ваш код: XXXX"`.

                    **Срок жизни кода:** 5 минут.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Код отправлен",
                    content = @Content(schema = @Schema(implementation = RegisterStartResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации или не принято согласие на ПД"),
            @ApiResponse(responseCode = "403", description = "Аккаунт заблокирован"),
            @ApiResponse(responseCode = "409", description = "Номер уже зарегистрирован")
    })
    @PostMapping("/register/start")
    public ResponseEntity<RegisterStartResponse> registerStart(
            @Valid @RequestBody RegisterStartRequest req,
            HttpServletRequest httpRequest) {
        String ip = extractClientIp(httpRequest);
        return ResponseEntity.ok(authService.registerStart(req, ip));
    }

    @Operation(
            summary = "Шаг 2 регистрации — подтвердить SMS-код",
            description = """
                    Проверяет код из SMS, активирует аккаунт и возвращает пару токенов.

                    После успеха пользователь может сразу пользоваться приложением.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Регистрация подтверждена",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Неверный/просроченный код или пользователь уже активирован")
    })
    @PostMapping("/register/confirm")
    public ResponseEntity<AuthResponse> registerConfirm(
            @Valid @RequestBody RegisterConfirmRequest req) {
        return ResponseEntity.ok(authService.registerConfirm(req));
    }

    @Operation(
            summary = "Вход по телефону и паролю",
            description = """
                    Возвращает пару токенов: access (JWT, 30 мин) и refresh (60 дней).

                    **Безопасность:** при неверном телефоне или пароле возвращается одна и та же ошибка —
                    чтобы нельзя было определить, зарегистрирован ли номер.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Успешный вход",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неверный телефон или пароль"),
            @ApiResponse(responseCode = "403", description = "Аккаунт не активирован / заблокирован / удалён")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @Operation(
            summary = "Обновить пару токенов",
            description = """
                    Использует refresh-токен, чтобы выдать новую пару.
                    Старый refresh **отзывается** (ротация). Повторно использовать его нельзя.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Новая пара токенов",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Refresh-токен недействителен или истёк"),
            @ApiResponse(responseCode = "403", description = "Аккаунт неактивен")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        return ResponseEntity.ok(authService.refresh(req));
    }

    @Operation(
            summary = "Выход — отозвать refresh-токен",
            description = """
                    Помечает refresh-токен как revoked.
                    Access-токен станет бесполезен только после истечения (до 30 минут).
                    """
    )
    @ApiResponse(responseCode = "200", description = "Выход выполнен")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@Valid @RequestBody LogoutRequest req) {
        authService.logout(req);
        return ResponseEntity.ok(Map.of("message", "Вы вышли из системы"));
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}