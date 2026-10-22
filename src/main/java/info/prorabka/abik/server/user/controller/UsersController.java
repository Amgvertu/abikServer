package info.prorabka.abik.server.user.controller;

import info.prorabka.abik.server.common.security.SecurityUtils;
import info.prorabka.abik.server.user.dto.*;
import info.prorabka.abik.server.user.entity.User;
import info.prorabka.abik.server.user.service.PhoneChangeService;
import info.prorabka.abik.server.user.service.UserProfileService;
import info.prorabka.abik.server.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Эндпоинты для пользователей и профилей.
 * Требуют авторизации (см. SecurityConfig).
 */
@RestController
@RequestMapping("/v1/users")
@Tag(name = "Users",
        description = "Профили пользователей. Все эндпоинты требуют access-токен.")
@SecurityRequirement(name = "bearerAuth")
public class UsersController {

    private final UserService userService;
    private final UserProfileService userProfileService;
    private final PhoneChangeService phoneChangeService;

    public UsersController(UserService userService, UserProfileService userProfileService, PhoneChangeService phoneChangeService) {
        this.userService = userService;
        this.userProfileService = userProfileService;
        this.phoneChangeService = phoneChangeService;
    }

    @Operation(
            summary = "Информация о текущем пользователе",
            description = """
                    Возвращает базовые данные аккаунта: id, телефон, статус, дату регистрации.
                    Пользователь определяется по access-токену.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Данные пользователя",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userService.getById(userId);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @Operation(
            summary = "Мой профиль",
            description = "Возвращает расширенный профиль текущего пользователя (ФИО, дата рождения, амплуа и т.д.)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Профиль",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "404", description = "Профиль ещё не создан")
    })
    @GetMapping("/me/profile")
    public ResponseEntity<UserProfileResponse> myProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userProfileService.getByUserId(userId));
    }

    @Operation(
            summary = "Создать или обновить мой профиль",
            description = """
                    Идемпотентный PUT: если профиля нет — создаётся, если есть — полностью перезаписывается.

                    **Обязательные поля:** cityId, lastName, firstName, middleName, birthDate,
                    heightCm, weightKg, stickHand, positions (хотя бы одно амплуа).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Профиль сохранён",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации полей"),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "404", description = "Указанный город не найден")
    })
    @PutMapping("/me/profile")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @Valid @RequestBody UserProfileRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userProfileService.createOrUpdate(userId, req));
    }

    @Operation(
            summary = "Профиль любого пользователя",
            description = "Публичная информация о профиле. По ТЗ «все видят всех»."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Профиль пользователя",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "Не авторизован"),
            @ApiResponse(responseCode = "404", description = "Пользователь или профиль не найден")
    })
    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserProfileResponse> profileOfUser(
            @Parameter(description = "ID пользователя", example = "1")
            @PathVariable Long userId) {
        return ResponseEntity.ok(userProfileService.getByUserId(userId));
    }

    // ============================================================
// Смена номера телефона
// ============================================================

    @Operation(summary = "Смена номера: запрос SMS на текущий номер")
    @PostMapping("/me/phone-change/start")
    public ResponseEntity<Map<String, String>> phoneChangeStart() {
        Long userId = SecurityUtils.getCurrentUserId();
        phoneChangeService.start(userId);
        return ResponseEntity.ok(Map.of("message", "SMS отправлено на текущий номер"));
    }

    @Operation(summary = "Смена номера: проверка кода старого номера")
    @PostMapping("/me/phone-change/verify-old")
    public ResponseEntity<Map<String, String>> phoneChangeVerifyOld(
            @Valid @RequestBody PhoneChangeVerifyOldRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(phoneChangeService.verifyOld(userId, req.code()));
    }

    @Operation(summary = "Смена номера: запрос SMS на новый номер")
    @PostMapping("/me/phone-change/send-new")
    public ResponseEntity<Map<String, String>> phoneChangeSendNew(
            @Valid @RequestBody PhoneChangeSendNewRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        phoneChangeService.sendNew(userId, req.changeToken(), req.newPhone());
        return ResponseEntity.ok(Map.of("message", "SMS отправлено на новый номер"));
    }

    @Operation(summary = "Смена номера: подтверждение нового номера")
    @PostMapping("/me/phone-change/confirm")
    public ResponseEntity<Map<String, String>> phoneChangeConfirm(
            @Valid @RequestBody PhoneChangeConfirmRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        phoneChangeService.confirm(userId, req.changeToken(), req.code());
        return ResponseEntity.ok(Map.of("message", "Номер изменён. Войдите заново."));
    }
}