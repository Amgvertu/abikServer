package info.prorabka.abik.server.team.dto;

import info.prorabka.abik.server.team.entity.MemberRole;
import info.prorabka.abik.server.team.entity.MemberStatus;
import info.prorabka.abik.server.team.entity.TeamMember;
import info.prorabka.abik.server.user.entity.UserPosition;

import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Представление участника команды.
 * Имя, фамилия, амплуа и аватар — из профиля, если он заполнен.
 */
public record TeamMemberResponse(
        Long id,
        Long userId,
        String phone,
        String lastName,
        String firstName,
        MemberRole role,
        MemberStatus status,
        Short jerseyNumber,
        Boolean isTrainer,
        Set<UserPosition> positions,   // ← НОВОЕ (BE-15): амплуа, пустое множество если профиль не заполнен
        String avatarUrl,              // ← НОВОЕ (BE-15): относительный URL аватара или null
        OffsetDateTime joinedAt
) {
    public static TeamMemberResponse from(TeamMember m,
                                          String phone,
                                          String lastName,
                                          String firstName,
                                          Set<UserPosition> positions,
                                          String avatarUrl) {
        return new TeamMemberResponse(
                m.getId(), m.getUserId(), phone, lastName, firstName,
                m.getRole(), m.getStatus(), m.getJerseyNumber(),
                m.getIsTrainer(),
                positions,
                avatarUrl,
                m.getJoinedAt()
        );
    }
}