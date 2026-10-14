package info.prorabka.abik.server.team.repository;

import info.prorabka.abik.server.team.entity.MemberRole;
import info.prorabka.abik.server.team.entity.MemberStatus;
import info.prorabka.abik.server.team.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    /** Все участники команды. */
    List<TeamMember> findByTeamIdOrderByJoinedAtAsc(Long teamId);

    /** Активные участники команды. */
    List<TeamMember> findByTeamIdAndStatusOrderByJoinedAtAsc(Long teamId, MemberStatus status);

    /** Найти запись участника в команде (вне зависимости от статуса). */
    Optional<TeamMember> findByTeamIdAndUserId(Long teamId, Long userId);

    /** Найти активного участника в команде. */
    Optional<TeamMember> findByTeamIdAndUserIdAndStatus(Long teamId, Long userId, MemberStatus status);

    /** Владелец команды. */
    Optional<TeamMember> findByTeamIdAndRoleAndStatus(Long teamId, MemberRole role, MemberStatus status);

    /** Проверить, что пользователь — активный участник команды. */
    boolean existsByTeamIdAndUserIdAndStatus(Long teamId, Long userId, MemberStatus status);

    /** Все команды, где пользователь — активный участник. */
    List<TeamMember> findByUserIdAndStatusOrderByJoinedAtDesc(Long userId, MemberStatus status);

    /** Проверить занятость игрового номера в команде. */
    boolean existsByTeamIdAndJerseyNumberAndStatus(Long teamId, Short jerseyNumber, MemberStatus status);

    /** Найти участника с заданным номером в команде (для проверки при обновлении). */
    Optional<TeamMember> findByTeamIdAndJerseyNumberAndStatus(
            Long teamId, Short jerseyNumber, MemberStatus status);

    /** Число активных модераторов (для бизнес-проверок). */
    long countByTeamIdAndRoleAndStatus(Long teamId, MemberRole role, MemberStatus status);
}