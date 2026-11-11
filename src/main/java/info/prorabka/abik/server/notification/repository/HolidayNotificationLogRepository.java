package info.prorabka.abik.server.notification.repository;

import info.prorabka.abik.server.notification.entity.HolidayNotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HolidayNotificationLogRepository extends JpaRepository<HolidayNotificationLog, Long> {
}
