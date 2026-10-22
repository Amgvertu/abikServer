package info.prorabka.abik.server.event.dto;

import java.util.List;

public record LineupSaveRequest(
        Long goalkeeperId,
        List<LineSaveItem> lines
) {
    public record LineSaveItem(
            List<Long> defenderIds,
            List<Long> forwardIds
    ) {}
}