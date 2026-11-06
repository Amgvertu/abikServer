package info.prorabka.abik.server.notification.repository;

import info.prorabka.abik.server.notification.entity.EventReminderLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventReminderLogRepository extends JpaRepository<EventReminderLog, Long> {
    boolean existsByUserIdAndEventIdAndReminderIndex(Long userId, Long eventId, Integer reminderIndex);
}
