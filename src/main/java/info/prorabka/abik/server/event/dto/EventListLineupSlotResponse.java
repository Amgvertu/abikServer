package info.prorabka.abik.server.event.dto;

import info.prorabka.abik.server.event.entity.LineupRole;

/** Слот звена в краткой карточке события, включая имя игрока. */
public record EventListLineupSlotResponse(
        Short lineNumber,
        Short slotNumber,
        LineupRole role,
        Long userId,
        String fullName
) {}
