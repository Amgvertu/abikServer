package info.prorabka.abik.server.roster.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record RosterReplaceRequest(@NotNull Set<Long> userIds) {}