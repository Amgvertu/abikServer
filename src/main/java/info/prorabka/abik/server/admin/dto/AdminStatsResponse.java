package info.prorabka.abik.server.admin.dto;

import java.time.LocalDate;
import java.util.List;

public record AdminStatsResponse(long totalUsers, long totalTeams,
                                 long usersToday, long usersWeek, long usersMonth,
                                 long teamsToday, long teamsWeek, long teamsMonth,
                                 List<DailyStat> last30Days) {
    public record DailyStat(LocalDate date, long users, long teams) {}
}
