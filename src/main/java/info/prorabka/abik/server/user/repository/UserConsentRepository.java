package info.prorabka.abik.server.user.repository;

import info.prorabka.abik.server.user.entity.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Репозиторий согласий на обработку ПД.
 */
@Repository
public interface UserConsentRepository extends JpaRepository<UserConsent, Long> {

    /** Все согласия конкретного пользователя, свежие сверху. */
    List<UserConsent> findByUserIdOrderByAcceptedAtDesc(Long userId);
}
