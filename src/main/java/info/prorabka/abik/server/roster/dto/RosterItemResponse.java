package info.prorabka.abik.server.roster.dto;

import info.prorabka.abik.server.user.entity.UserPosition;

import java.time.OffsetDateTime;
import java.util.Set;

public record RosterItemResponse(
        Long userId,
        String fullName,
        Set<UserPosition> positions,
        String status,
        OffsetDateTime addedAt
) {}