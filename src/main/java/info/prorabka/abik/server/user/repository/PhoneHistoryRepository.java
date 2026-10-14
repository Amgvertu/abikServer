package info.prorabka.abik.server.user.repository;

import info.prorabka.abik.server.user.entity.PhoneHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Репозиторий истории смены номеров.
 */
@Repository
public interface PhoneHistoryRepository extends JpaRepository<PhoneHistory, Long> {

    /** История телефонов пользователя, свежие сверху. */
    List<PhoneHistory> findByUserIdOrderByChangedAtDesc(Long userId);
}