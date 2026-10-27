package info.prorabka.abik.server.event.dto;

import java.util.List;

/**
 * Заявка на игру. Слоты всегда отсортированы:
 * сначала вратарь, затем звенья по номеру, внутри — защитники, потом нападающие по slotNumber.
 */
public record LineupDto(
        List<LineupSlotDto> slots
) {}