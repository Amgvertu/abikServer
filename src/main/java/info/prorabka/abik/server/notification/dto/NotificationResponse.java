package info.prorabka.abik.server.notification.dto;

import info.prorabka.abik.server.notification.entity.Notification;
import info.prorabka.abik.server.notification.entity.NotificationType;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Представление уведомления для клиента.
 */
public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String body,
        Long teamId,
        Map<String, Object> payload,
        Boolean isRead,
        OffsetDateTime sentAt,
        OffsetDateTime readAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getBody(),
                n.getTeamId(),
                n.getPayload(),
                n.getIsRead(),
                n.getSentAt(),
                n.getReadAt()
        );
    }
}