package info.prorabka.abik.server.event.dto;

import info.prorabka.abik.server.event.entity.EventType;
import info.prorabka.abik.server.event.entity.VoteAnswer;

import java.time.LocalDate;
import java.util.List;

public record EventDetailResponse(
        Long id,
        EventType type,
        LocalDate date,
        String time,
        String place,
        String note,
        String status,
        VoteAnswer myVote,
        List<EventVoterDto> voters,
        LineupDto lineup
) {}