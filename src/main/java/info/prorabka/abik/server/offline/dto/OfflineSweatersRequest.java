package info.prorabka.abik.server.offline.dto;

import info.prorabka.abik.server.event.dto.SweaterColorItemDto;

import java.util.List;

public record OfflineSweatersRequest(Long eventId, List<SweaterColorItemDto> items) {}
