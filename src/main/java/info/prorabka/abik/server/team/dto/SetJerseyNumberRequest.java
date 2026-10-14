package info.prorabka.abik.server.team.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Запрос на установку/снятие игрового номера.
 * Если jerseyNumber = null — номер снимается.
 */
public record SetJerseyNumberRequest(

        @Min(value = 1, message = "Номер от 1 до 99")
        @Max(value = 99, message = "Номер от 1 до 99")
        Short jerseyNumber
) {}