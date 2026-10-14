package info.prorabka.abik.server.auth.repository;

import info.prorabka.abik.server.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Репозиторий refresh-токенов.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /** Найти активный (не отозванный) токен по его хешу. */
    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    /** Отозвать все активные токены пользователя — при logout со всех устройств. */
    @Modifying
    @Query("""
            UPDATE RefreshToken t
            SET t.revoked = true, t.revokedAt = :now
            WHERE t.userId = :userId AND t.revoked = false
            """)
    int revokeAllByUserId(@Param("userId") Long userId,
                          @Param("now") OffsetDateTime now);
}