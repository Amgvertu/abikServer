package info.prorabka.abik.server.team.repository;

import info.prorabka.abik.server.team.entity.InvitationStatus;
import info.prorabka.abik.server.team.entity.TeamInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, Long> {

    /** Все приглашения команды с заданным статусом. */
    List<TeamInvitation> findByTeamIdAndStatusOrderByCreatedAtDesc(Long teamId, InvitationStatus status);

    /** Все приглашения конкретному пользователю (например, PENDING). */
    List<TeamInvitation> findByInvitedUserIdAndStatusOrderByCreatedAtDesc(
            Long invitedUserId, InvitationStatus status);

    /** Найти активное приглашение (PENDING) от команды пользователю. */
    Optional<TeamInvitation> findByTeamIdAndInvitedUserIdAndStatus(
            Long teamId, Long invitedUserId, InvitationStatus status);

    /** Пометить все просроченные PENDING-приглашения как EXPIRED. */
    @Modifying
    @Query("""
            UPDATE TeamInvitation i
            SET i.status = info.prorabka.abik.server.team.entity.InvitationStatus.EXPIRED,
                i.respondedAt = :now
            WHERE i.status = info.prorabka.abik.server.team.entity.InvitationStatus.PENDING
              AND i.expiresAt < :now
            """)
    int expireOverdue(@Param("now") OffsetDateTime now);
}