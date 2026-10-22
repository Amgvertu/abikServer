package info.prorabka.abik.server.event.dto;

import java.util.List;

public record LineupDto(
        Long goalkeeperId,
        List<LineDto> lines
) {}