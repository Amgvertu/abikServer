package info.prorabka.abik.server.offline.dto;

import info.prorabka.abik.server.event.dto.LineupSlotDto;

import java.util.List;

public record OfflineLineupRequest(Long eventId, List<LineupSlotDto> slots) {}
