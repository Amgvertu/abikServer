package info.prorabka.abik.server.team.dto;

import info.prorabka.abik.server.team.entity.MemberRole;
import info.prorabka.abik.server.team.entity.MemberStatus;
import info.prorabka.abik.server.team.entity.TeamMember;

import java.time.OffsetDateTime;

/**
 * Представление участника команды.
 * Имя, фамилия — из профиля, если он заполнен.
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
        OffsetDateTime joinedAt
) {
    public static TeamMemberResponse from(TeamMember m,
                                          String phone,
                                          String lastName,
                                          String firstName) {
        return new TeamMemberResponse(
                m.getId(),
                m.getUserId(),
                phone,
                lastName,
                firstName,
                m.getRole(),
                m.getStatus(),
                m.getJerseyNumber(),
                m.getJoinedAt()
        );
    }
}