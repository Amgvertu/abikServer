package info.prorabka.abik.server.event.dto;

import info.prorabka.abik.server.event.entity.SweaterColor;
import info.prorabka.abik.server.event.entity.VoteAnswer;
import info.prorabka.abik.server.user.entity.UserPosition;

import java.util.Set;

public record EventVoterDto(
        Long userId,
        String fullName,
        VoteAnswer answer,
        Set<UserPosition> positions,
        Boolean isTrainer,
        SweaterColor sweaterColor
) {}