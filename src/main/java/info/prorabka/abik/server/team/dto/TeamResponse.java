package info.prorabka.abik.server.team.dto;

import info.prorabka.abik.server.team.entity.Team;
import info.prorabka.abik.server.team.entity.TeamStatus;

import java.time.OffsetDateTime;

/**
 * Краткое представление команды (для списков).
 */
public record TeamResponse(
        Long id,
        String name,
        Long cityId,
        String cityName,
        Long homeRegionId,
        String homeRegionName,
        String logoUrl,
        TeamStatus status,
        OffsetDateTime createdAt
) {
    public static TeamResponse from(Team t, String cityName, String regionName) {
        return new TeamResponse(
                t.getId(),
                t.getName(),
                t.getCityId(),
                cityName,
                t.getHomeRegionId(),
                regionName,
                t.getLogoUrl(),
                t.getStatus(),
                t.getCreatedAt()
        );
    }
}