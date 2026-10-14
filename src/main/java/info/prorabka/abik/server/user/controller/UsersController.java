package info.prorabka.abik.server.user.controller;

import info.prorabka.abik.server.common.security.SecurityUtils;
import info.prorabka.abik.server.user.dto.UserResponse;
import info.prorabka.abik.server.user.entity.User;
import info.prorabka.abik.server.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Эндпоинты для работы с пользователями.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UsersController {

    private final UserService userService;

    public UsersController(UserService userService) {
        this.userService = userService;
    }

    /** Получить информацию о текущем пользователе (по access-токену). */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userService.getById(userId);
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
