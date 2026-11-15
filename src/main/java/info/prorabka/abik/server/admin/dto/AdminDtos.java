package info.prorabka.abik.server.admin.dto;

import java.time.OffsetDateTime;
import java.util.List;

/** DTO расширенной админки. Суммы всегда передаются в копейках. */
public final class AdminDtos {
    private AdminDtos() {}

    public record DailyStat(String date, long users, long teams) {}
    public record Overview(
            long totalUsers, long totalTeams,
            long chargeCount, long chargeAmountKopecks,
            long cancelledChargeCount, long cancelledChargeAmountKopecks,
            long paymentCount, long paymentAmountKopecks,
            List<DailyStat> daily
    ) {}
    public record UserRow(
            Long id, String phone, String status, boolean superAdmin, boolean adAdmin,
            String fullName, Long cityId, String cityName, Long regionId, String regionName,
            List<String> teams, List<String> positions, OffsetDateTime createdAt, String blockReason
    ) {}
    public record TeamFinance(Long teamId, String teamName, String role, long balanceKopecks,
                              long chargeCount, long chargeAmountKopecks,
                              long paymentCount, long paymentAmountKopecks) {}
    public record UserDetails(UserRow user, List<TeamFinance> teams) {}
    public record TeamRow(
            Long id, String name, String status, Long cityId, String cityName,
            Long regionId, String regionName, OffsetDateTime createdAt, String blockReason,
            long membersCount, long eventsCount, long chargeAmountKopecks, long paymentAmountKopecks
    ) {}
    public record MemberRow(Long userId, String fullName, String phone, String role,
                            boolean trainer, List<String> positions, long balanceKopecks) {}
    public record EventRow(Long id, String title, String section, String date, String time, String place, String status) {}
    public record TeamDetails(TeamRow team, List<MemberRow> members, List<EventRow> events) {}
    public record MessageRow(Long id, String title, String body, String linkUrl, String audience,
                             long recipients, long read, OffsetDateTime createdAt, String author) {}
    public record RegistrationEventRow(Long id, String requestId, String phoneMasked, String stage,
                                       String status, String code, String details, String client,
                                       OffsetDateTime occurredAt) {}
    public record AdvertisementRow(
            Long id, String title, String body, String imageUrl, String linkUrl, String buttonText,
            String targetType, Long targetId, String targetName,
            OffsetDateTime startsAt, OffsetDateTime endsAt, boolean active,
            Long creatorUserId, String creatorName, OffsetDateTime createdAt
    ) {}
    public record AdTotals(long views, long viewers, long clicks, long uniqueClicks) {}
    public record AdGeoRow(Long regionId, String regionName, Long cityId, String cityName,
                           Long teamId, String teamName, long views, long viewers,
                           long clicks, long uniqueClicks) {}
    public record AdStats(Long adId, AdTotals totals, List<AdGeoRow> geography) {}
    public record TargetOption(Long id, String name, Long regionId, Long cityId) {}
}
