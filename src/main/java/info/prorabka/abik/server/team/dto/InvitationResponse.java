package info.prorabka.abik.server.team.dto;

import info.prorabka.abik.server.team.entity.InvitationStatus;
import info.prorabka.abik.server.team.entity.TeamInvitation;

import java.time.OffsetDateTime;

/**
 * Представление приглашения в команду.
 */
public record InvitationResponse(
        Long id,
        Long teamId,
        String teamName,
        Long inviterUserId,
        Long invitedUserId,
        String invitedPhone,
        InvitationStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt,
        OffsetDateTime respondedAt
) {
    public static InvitationResponse from(TeamInvitation i, String teamName) {
        return new InvitationResponse(
                i.getId(),
                i.getTeamId(),
                teamName,
                i.getInviterUserId(),
                i.getInvitedUserId(),
                i.getInvitedPhone(),
                i.getStatus(),
                i.getCreatedAt(),
                i.getExpiresAt(),
                i.getRespondedAt()
        );
    }
}