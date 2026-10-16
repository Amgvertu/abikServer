package info.prorabka.abik.server.notification.repository;

import info.prorabka.abik.server.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Все уведомления пользователя, свежие сверху. */
    List<Notification> findByUserIdOrderBySentAtDesc(Long userId);

    /** Непрочитанные уведомления пользователя. */
    List<Notification> findByUserIdAndIsReadFalseOrderBySentAtDesc(Long userId);

    /** Число непрочитанных. */
    long countByUserIdAndIsReadFalse(Long userId);

    /** Пометить все непрочитанные уведомления пользователя как прочитанные. */
    @Modifying
    @Query("""
            UPDATE Notification n
            SET n.isRead = true, n.readAt = :now
            WHERE n.userId = :userId AND n.isRead = false
            """)
    int markAllReadByUserId(@Param("userId") Long userId, @Param("now") OffsetDateTime now);
}