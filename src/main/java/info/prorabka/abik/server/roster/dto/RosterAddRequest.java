package info.prorabka.abik.server.roster.dto;

import jakarta.validation.constraints.NotNull;

public record RosterAddRequest(
        @NotNull Long userId,
        Boolean chargeEntryFee,
        EntryFeeDto entryFee
) {}