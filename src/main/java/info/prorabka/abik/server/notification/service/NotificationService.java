package info.prorabka.abik.server.notification.service;

import info.prorabka.abik.server.common.exception.ForbiddenException;
import info.prorabka.abik.server.common.exception.NotFoundException;
import info.prorabka.abik.server.notification.dto.NotificationResponse;
import info.prorabka.abik.server.notification.entity.Notification;
import info.prorabka.abik.server.notification.entity.NotificationType;
import info.prorabka.abik.server.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Сервис уведомлений.
 * Основной поток: сохранить запись в БД + отправить push через PushSender.
 * Отправка никогда не пробрасывает исключение наверх —
 * бизнес-операция не должна падать из-за проблем с push.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    /** Целевая платформа по умолчанию — мобильный клиент. */
    public static final String TARGET_CLIENT = "abik-client";
    /** Целевая платформа — самодельный SMS-шлюз (на будущее). */
    public static final String TARGET_SMS_GATEWAY = "abik-sms-gateway";

    private final NotificationRepository notificationRepository;
    private final PushSender pushSender;

    public NotificationService(NotificationRepository notificationRepository,
                               PushSender pushSender) {
        this.notificationRepository = notificationRepository;
        this.pushSender = pushSender;
    }

    // ============================================================
    // Отправка
    // ============================================================

    @Transactional
    public void send(Long userId, NotificationType type, String title, String body,
                     Long teamId, Map<String, Object> payload) {
        send(userId, TARGET_CLIENT, type, title, body, teamId, payload);
    }

    @Transactional
    public void send(Long userId, String target, NotificationType type,
                     String title, String body, Long teamId, Map<String, Object> payload) {
        try {
            Notification entity = Notification.builder()
                    .userId(userId)
                    .type(type)
                    .title(title)
                    .body(body)
                    .teamId(teamId)
                    .payload(payload)
                    .isRead(false)
                    .deliveryStatus("PENDING")
                    .build();
            Notification saved = notificationRepository.save(entity);

            boolean sent = pushSender.send(userId, target, type, title, body, payload);
            saved.setDeliveryStatus(sent ? "SENT" : "FAILED");
            notificationRepository.save(saved);

        } catch (Exception e) {
            log.error("Ошибка отправки уведомления userId={}, type={}: {}",
                    userId, type, e.getMessage(), e);
        }
    }

    // ============================================================
    // Чтение
    // ============================================================

    public List<NotificationResponse> listMy(Long userId, boolean onlyUnread) {
        List<Notification> notifications = onlyUnread
                ? notificationRepository.findByUserIdAndIsReadFalseOrderBySentAtDesc(userId)
                : notificationRepository.findByUserIdOrderBySentAtDesc(userId);

        return notifications.stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markRead(Long notificationId, Long userId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Уведомление не найдено"));

        if (!n.getUserId().equals(userId)) {
            throw new ForbiddenException("Уведомление адресовано другому пользователю");
        }

        if (Boolean.FALSE.equals(n.getIsRead())) {
            n.setIsRead(true);
            n.setReadAt(OffsetDateTime.now());
            n = notificationRepository.save(n);
        }
        return NotificationResponse.from(n);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepository.markAllReadByUserId(userId, OffsetDateTime.now());
    }

    // ============================================================
    // Фасадные методы — для вызова из других сервисов
    // ============================================================

    @Transactional
    public void notifyInvitation(Long invitedUserId, Long teamId, String teamName,
                                 String inviterName) {
        send(invitedUserId, NotificationType.INVITATION,
                "Приглашение в команду",
                inviterName + " приглашает вас в команду «" + teamName + "»",
                teamId,
                Map.of("teamId", teamId, "teamName", teamName));
    }

    @Transactional
    public void notifyInvitationAccepted(Long inviterUserId, Long teamId, String teamName,
                                         String acceptedByName) {
        send(inviterUserId, NotificationType.INVITATION,
                "Приглашение принято",
                acceptedByName + " принял(а) приглашение в команду «" + teamName + "»",
                teamId,
                Map.of("teamId", teamId, "teamName", teamName));
    }

    @Transactional
    public void notifyRoleChanged(Long targetUserId, Long teamId, String teamName,
                                  String newRole) {
        send(targetUserId, NotificationType.ROLE_CHANGED,
                "Изменение роли",
                "Ваша роль в команде «" + teamName + "» изменена на " + newRole,
                teamId,
                Map.of("teamId", teamId, "newRole", newRole));
    }

    @Transactional
    public void notifyNewCharge(Long userId, Long teamId, String teamName,
                                Long chargeId, long amountKopecks, String description) {
        send(userId, NotificationType.NEW_CHARGE,
                "Новое начисление",
                "В команде «" + teamName + "»: " + description + " — " + rub(amountKopecks),
                teamId,
                Map.of("teamId", teamId, "chargeId", chargeId, "amountKopecks", amountKopecks));
    }

    @Transactional
    public void notifyPayment(Long userId, Long teamId, String teamName,
                              long amountKopecks, String byName) {
        send(userId, NotificationType.PAYMENT,
                "Платёж принят",
                byName + " внёс платёж " + rub(amountKopecks) + " в команду «" + teamName + "»",
                teamId,
                Map.of("teamId", teamId, "amountKopecks", amountKopecks));
    }

    @Transactional
    public void notifyDebtReminder(Long userId, Long teamId, String teamName, long debtKopecks) {
        send(userId, NotificationType.DEBT_REMINDER,
                "Напоминание о долге",
                "Ваш долг в команде «" + teamName + "»: " + rub(debtKopecks),
                teamId,
                Map.of("teamId", teamId, "debtKopecks", debtKopecks));
    }

    @Transactional
    public void notifyOwnershipTransferred(Long newOwnerUserId, Long formerOwnerUserId,
                                           Long teamId, String teamName) {
        send(newOwnerUserId, NotificationType.OWNERSHIP_TRANSFER,
                "Вы — владелец команды",
                "Владение командой «" + teamName + "» передано вам",
                teamId,
                Map.of("teamId", teamId, "role", "OWNER"));

        send(formerOwnerUserId, NotificationType.OWNERSHIP_TRANSFER,
                "Владение передано",
                "Вы передали владение командой «" + teamName + "»",
                teamId,
                Map.of("teamId", teamId, "role", "MODERATOR"));
    }

    @Transactional
    public void notifyExcluded(Long userId, Long teamId, String teamName) {
        send(userId, NotificationType.EXCLUDED,
                "Исключение из команды",
                "Вы исключены из команды «" + teamName + "»",
                teamId,
                Map.of("teamId", teamId));
    }

    @Transactional
    public void notifyTeamArchived(Long userId, Long teamId, String teamName) {
        send(userId, NotificationType.ARCHIVED,
                "Команда архивирована",
                "Команда «" + teamName + "» архивирована",
                teamId,
                Map.of("teamId", teamId));
    }

    // ============================================================
    // Helpers
    // ============================================================

    private String rub(long kopecks) {
        return String.format("%.2f ₽", kopecks / 100.0);
    }
}