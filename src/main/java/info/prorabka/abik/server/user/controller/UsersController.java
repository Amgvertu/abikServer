package info.prorabka.abik.server.user.controller;

import info.prorabka.abik.server.common.exception.ForbiddenException;
import info.prorabka.abik.server.common.security.SecurityUtils;
import info.prorabka.abik.server.user.dto.UserProfileRequest;
import info.prorabka.abik.server.user.dto.UserProfileResponse;
import info.prorabka.abik.server.user.dto.UserResponse;
import info.prorabka.abik.server.user.entity.User;
import info.prorabka.abik.server.user.service.UserProfileService;
import info.prorabka.abik.server.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Эндпоинты для пользователей и профилей.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UsersController {

    private final UserService userService;
    private final UserProfileService userProfileService;

    public UsersController(UserService userService, UserProfileService userProfileService) {
        this.userService = userService;
        this.userProfileService = userProfileService;
    }

    /** Информация о текущем пользователе (по access-токену). */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userService.getById(userId);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    /** Мой профиль. */
    @GetMapping("/me/profile")
    public ResponseEntity<UserProfileResponse> myProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userProfileService.getByUserId(userId));
    }

    /** Создать или обновить мой профиль. */
    @PutMapping("/me/profile")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @Valid @RequestBody UserProfileRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userProfileService.createOrUpdate(userId, req));
    }

    /**
     * Публичный профиль любого пользователя.
     * Все видят всех (по ТЗ, раздел 4.6).
     */
    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserProfileResponse> profileOfUser(@PathVariable Long userId) {
        return ResponseEntity.ok(userProfileService.getByUserId(userId));
    }
}