package info.prorabka.abik.server.charge.dto;

import info.prorabka.abik.server.charge.entity.SubscriptionPollAnswer;
import jakarta.validation.constraints.NotNull;

public record SubscriptionPollVoteRequest(
        @NotNull(message = "Выберите ответ") SubscriptionPollAnswer answer
) {}
