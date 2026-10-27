package info.prorabka.abik.server.event.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record LineupSaveRequest(
        @NotNull List<@Valid LineupSlotDto> slots
) {}