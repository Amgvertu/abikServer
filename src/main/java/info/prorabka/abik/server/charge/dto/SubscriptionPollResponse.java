package info.prorabka.abik.server.charge.dto;

import info.prorabka.abik.server.charge.entity.SubscriptionPollAnswer;

import java.time.LocalDate;
import java.util.List;

public record SubscriptionPollResponse(
        Long id,
        Long templateId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate validFrom,
        LocalDate validUntil,
        Integer yesCount,
        Integer noCount,
        SubscriptionPollAnswer myVote,
        List<SubscriptionPollVoterResponse> positiveVoters
) {
    public record SubscriptionPollVoterResponse(Long userId, String name) {}
}
