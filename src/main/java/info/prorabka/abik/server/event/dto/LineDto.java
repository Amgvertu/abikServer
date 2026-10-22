package info.prorabka.abik.server.event.dto;

import java.util.List;

public record LineDto(
        Integer lineNumber,
        List<Long> defenderIds,
        List<Long> forwardIds
) {}