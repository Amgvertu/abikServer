package info.prorabka.abik.server.roster.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record EntryFeeDto(
        @NotNull Long groupId,
        Long subgroupId,
        @NotNull @Min(1) Long amountKopecks,
        @NotNull LocalDate dueDate
) {}