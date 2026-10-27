package info.prorabka.abik.server.event.dto;

import info.prorabka.abik.server.event.entity.EventSection;
import info.prorabka.abik.server.event.entity.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EventUpdateRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull EventSection section,
        @NotNull EventType type,
        @NotNull LocalDate date,
        @NotBlank @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$") String time,
        @NotBlank @Size(max = 500) String place,
        @Size(max = 1000) String note
) {}