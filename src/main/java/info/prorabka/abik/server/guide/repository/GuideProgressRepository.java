package info.prorabka.abik.server.guide.repository;

import info.prorabka.abik.server.guide.entity.GuideProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GuideProgressRepository extends JpaRepository<GuideProgress, Long> {
    List<GuideProgress> findAllByUserId(Long userId);
    Optional<GuideProgress> findByUserIdAndScreenKeyAndRoleKey(Long userId, String screenKey, String roleKey);
}
