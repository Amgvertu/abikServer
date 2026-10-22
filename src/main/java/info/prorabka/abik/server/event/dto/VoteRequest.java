package info.prorabka.abik.server.event.dto;

import info.prorabka.abik.server.event.entity.VoteAnswer;
import jakarta.validation.constraints.NotNull;

public record VoteRequest(@NotNull VoteAnswer answer) {}