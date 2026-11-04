package info.prorabka.abik.server.release.repository;

import info.prorabka.abik.server.release.entity.AppRelease;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AppReleaseRepository extends JpaRepository<AppRelease, Long> {
    Optional<AppRelease> findFirstByPlatformOrderByVersionCodeDesc(String platform);
    Optional<AppRelease> findByPlatformAndVersionCode(String platform, Integer versionCode);
}
