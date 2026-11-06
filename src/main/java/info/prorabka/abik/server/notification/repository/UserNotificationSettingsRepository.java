package info.prorabka.abik.server.notification.repository;

import info.prorabka.abik.server.notification.entity.UserNotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserNotificationSettingsRepository extends JpaRepository<UserNotificationSettings, Long> {
}
