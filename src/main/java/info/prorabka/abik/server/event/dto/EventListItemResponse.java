package info.prorabka.abik.server.event.dto;

import info.prorabka.abik.server.event.entity.EventSection;
import info.prorabka.abik.server.event.entity.EventType;
import info.prorabka.abik.server.event.entity.VoteAnswer;

import java.time.LocalDate;

public record EventListItemResponse(
        Long id,
        String title,
        EventSection section,
        EventType type,
        LocalDate date,
        String time,
        String place,
        VoteAnswer myVote,
        long votesYes,
        long votesNo,
        long votesMaybe
) {}