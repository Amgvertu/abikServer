package info.prorabka.abik.server.event.dto;

import info.prorabka.abik.server.event.entity.LineupRole;
import jakarta.validation.constraints.NotNull;

/**
 * Один слот заявки на игру.
 * lineNumber — номер звена (null для вратаря),
 * slotNumber — номер слота: вратарь 1; защитники 1..2; нападающие 1..3.
 */
public record LineupSlotDto(
        Integer lineNumber,
        @NotNull Integer slotNumber,
        @NotNull LineupRole role,
        @NotNull Long userId
) {}