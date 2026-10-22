package info.prorabka.abik.server.notification.controller;

import info.prorabka.abik.server.common.security.SecurityUtils;
import info.prorabka.abik.server.notification.dto.NotificationResponse;
import info.prorabka.abik.server.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/notifications")
@Tag(name = "Notifications", description = "Уведомления текущего пользователя")
@SecurityRequirement(name = "bearerAuth")
public class NotificationsController {

    private final NotificationService notificationService;

    public NotificationsController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(summary = "Мои уведомления")
    @ApiResponse(responseCode = "200", description = "Список уведомлений",
            content = @Content(schema = @Schema(implementation = NotificationResponse.class)))
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list(
            @Parameter(description = "Только непрочитанные", example = "false")
            @RequestParam(name = "onlyUnread", required = false, defaultValue = "false")
            Boolean onlyUnread) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(notificationService.listMy(userId, Boolean.TRUE.equals(onlyUnread)));
    }

    @Operation(summary = "Число непрочитанных")
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(Map.of("count", notificationService.countUnread(userId)));
    }

    @Operation(summary = "Пометить прочитанным")
    @PostMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(
            @Parameter(description = "ID уведомления") @PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(notificationService.markRead(id, userId));
    }

    @Operation(summary = "Пометить все прочитанными")
    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllRead() {
        Long userId = SecurityUtils.getCurrentUserId();
        int updated = notificationService.markAllRead(userId);
        return ResponseEntity.ok(Map.of("updated", updated));
    }
}