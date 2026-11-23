package info.prorabka.abik.server.charge.dto;

import java.time.LocalDate;

/** Действующий абонемент пользователя, полученный по итогам опроса. */
public record ActiveSubscriptionResponse(
        Long teamId,
        String teamName,
        String title,
        LocalDate validFrom,
        LocalDate validUntil
) {}
