package info.prorabka.abik.server.team.dto;

import info.prorabka.abik.server.team.entity.MemberRole;
import info.prorabka.abik.server.team.entity.Team;
import info.prorabka.abik.server.team.entity.TeamStatus;

import java.time.OffsetDateTime;

/**
 * Подробное представление команды (при открытии карточки).
 * Содержит роль текущего пользователя и число активных участников.
 */
public record TeamDetailResponse(
        Long id,
        String name,
        Long cityId,
        String cityName,
        Long homeRegionId,
        String homeRegionName,
        String logoUrl,
        String description,
        String season,
        TeamStatus status,
        MemberRole myRole,
        long membersCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime archivedAt
) {
    public static TeamDetailResponse from(Team t, String cityName, String regionName,
                                          MemberRole myRole, long membersCount) {
        return new TeamDetailResponse(
                t.getId(),
                t.getName(),
                t.getCityId(),
                cityName,
                t.getHomeRegionId(),
                regionName,
                t.getLogoUrl(),
                t.getDescription(),
                t.getSeason(),
                t.getStatus(),
                myRole,
                membersCount,
                t.getCreatedAt(),
                t.getUpdatedAt(),
                t.getArchivedAt()
        );
    }
}