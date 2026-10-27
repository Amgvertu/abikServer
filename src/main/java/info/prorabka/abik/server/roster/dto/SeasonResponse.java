package info.prorabka.abik.server.roster.dto;

/**
 * Сезон команды с признаком текущего.
 * current = true, если сезон совпадает с полем season команды.
 */
public record SeasonResponse(
        String season,
        boolean current
) {}