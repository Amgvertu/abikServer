package info.prorabka.abik.server.event.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Set;

public record RecurrenceDto(
        @NotEmpty Set<@Min(1) @Max(7) Short> weekdays,
        @NotNull LocalDate untilDate
) {}