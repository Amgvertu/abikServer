package info.prorabka.abik.server.admin.service;

import info.prorabka.abik.server.admin.dto.AdminDtos.*;
import info.prorabka.abik.server.auth.service.RefreshTokenService;
import info.prorabka.abik.server.common.exception.BadRequestException;
import info.prorabka.abik.server.common.exception.ConflictException;
import info.prorabka.abik.server.common.exception.ForbiddenException;
import info.prorabka.abik.server.common.exception.NotFoundException;
import info.prorabka.abik.server.notification.entity.NotificationType;
import info.prorabka.abik.server.notification.service.NotificationService;
import info.prorabka.abik.server.team.entity.MemberStatus;
import info.prorabka.abik.server.team.entity.Team;
import info.prorabka.abik.server.team.entity.TeamStatus;
import info.prorabka.abik.server.team.repository.TeamMemberRepository;
import info.prorabka.abik.server.team.repository.TeamRepository;
import info.prorabka.abik.server.user.entity.User;
import info.prorabka.abik.server.user.entity.UserStatus;
import info.prorabka.abik.server.user.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class AdminOperationsService {
    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final TeamRepository teams;
    private final TeamMemberRepository members;
    private final RefreshTokenService refreshTokens;
    private final NotificationService notifications;

    public AdminOperationsService(JdbcTemplate jdbc, UserRepository users, TeamRepository teams,
                                  TeamMemberRepository members, RefreshTokenService refreshTokens,
                                  NotificationService notifications) {
        this.jdbc = jdbc; this.users = users; this.teams = teams; this.members = members;
        this.refreshTokens = refreshTokens; this.notifications = notifications;
    }

    public Overview overview(LocalDate from, LocalDate to, Long regionId, Long cityId, Long teamId) {
        LocalDate effectiveTo = to == null ? LocalDate.now() : to;
        LocalDate effectiveFrom = from == null ? effectiveTo.minusDays(29) : from;
        if (effectiveFrom.isAfter(effectiveTo)) throw new BadRequestException("Дата начала позже даты окончания");
        long userCount = countUsers(effectiveFrom, effectiveTo, regionId, cityId, teamId, false);
        long teamCount = countTeams(effectiveFrom, effectiveTo, regionId, cityId, teamId, false);
        Map<String, Object> finance = financeTotals(effectiveFrom, effectiveTo, regionId, cityId, teamId);
        List<DailyStat> daily = new ArrayList<>();
        for (LocalDate day = effectiveFrom; !day.isAfter(effectiveTo); day = day.plusDays(1)) {
            daily.add(new DailyStat(day.toString(),
                    countUsers(day, day, regionId, cityId, teamId, false),
                    countTeams(day, day, regionId, cityId, teamId, false)));
        }
        return new Overview(userCount, teamCount,
                number(finance.get("charge_count")), number(finance.get("charge_sum")),
                number(finance.get("cancelled_count")), number(finance.get("cancelled_sum")),
                number(finance.get("payment_count")), number(finance.get("payment_sum")), daily);
    }

    private long countUsers(LocalDate from, LocalDate to, Long regionId, Long cityId, Long teamId, boolean ignored) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(DISTINCT u.id) FROM users u LEFT JOIN user_profiles p ON p.user_id=u.id LEFT JOIN cities c ON c.id=p.city_id LEFT JOIN team_members tm ON tm.user_id=u.id AND tm.status='ACTIVE' LEFT JOIN teams t ON t.id=tm.team_id WHERE u.status<>'DELETED' AND (u.created_at AT TIME ZONE 'UTC')::date BETWEEN ? AND ?");
        List<Object> args = new ArrayList<>(List.of(from, to));
        addGeoFilters(sql, args, regionId, cityId, teamId, "c", "t");
        Long value = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return value == null ? 0 : value;
    }

    private long countTeams(LocalDate from, LocalDate to, Long regionId, Long cityId, Long teamId, boolean ignored) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM teams t WHERE t.status<>'ARCHIVED' AND (t.created_at AT TIME ZONE 'UTC')::date BETWEEN ? AND ?");
        List<Object> args = new ArrayList<>(List.of(from, to));
        if (regionId != null) { sql.append(" AND t.home_region_id=?"); args.add(regionId); }
        if (cityId != null) { sql.append(" AND t.city_id=?"); args.add(cityId); }
        if (teamId != null) { sql.append(" AND t.id=?"); args.add(teamId); }
        Long value = jdbc.queryForObject(sql.toString(), Long.class, args.toArray());
        return value == null ? 0 : value;
    }

    private Map<String, Object> financeTotals(LocalDate from, LocalDate to, Long regionId, Long cityId, Long teamId) {
        String base = " FROM teams t LEFT JOIN cities c ON c.id=t.city_id WHERE t.status<>'ARCHIVED'";
        StringBuilder teamFilter = new StringBuilder();
        List<Object> filterArgs = new ArrayList<>();
        addGeoFilters(teamFilter, filterArgs, regionId, cityId, teamId, "c", "t");
        String chargesSql = "SELECT COUNT(*) FILTER (WHERE ch.status<>'CANCELLED') charge_count, COALESCE(SUM(ch.amount_kopecks) FILTER (WHERE ch.status<>'CANCELLED'),0) charge_sum, COUNT(*) FILTER (WHERE ch.status='CANCELLED') cancelled_count, COALESCE(SUM(ch.amount_kopecks) FILTER (WHERE ch.status='CANCELLED'),0) cancelled_sum FROM charges ch JOIN users u ON u.id=ch.user_id JOIN teams t ON t.id=ch.team_id LEFT JOIN cities c ON c.id=t.city_id WHERE u.status<>'DELETED' AND t.status<>'ARCHIVED' AND (ch.created_at AT TIME ZONE 'UTC')::date BETWEEN ? AND ?" + teamFilter;
        String paymentsSql = "SELECT COUNT(*) payment_count, COALESCE(SUM(p.amount_kopecks),0) payment_sum FROM payments p JOIN users u ON u.id=p.user_id JOIN teams t ON t.id=p.team_id LEFT JOIN cities c ON c.id=t.city_id WHERE p.deleted_at IS NULL AND u.status<>'DELETED' AND t.status<>'ARCHIVED' AND (p.created_at AT TIME ZONE 'UTC')::date BETWEEN ? AND ?" + teamFilter;
        List<Object> chargeArgs = new ArrayList<>(List.of(from, to)); chargeArgs.addAll(filterArgs);
        List<Object> paymentArgs = new ArrayList<>(List.of(from, to)); paymentArgs.addAll(filterArgs);
        Map<String, Object> result = jdbc.queryForMap(chargesSql, chargeArgs.toArray());
        result.putAll(jdbc.queryForMap(paymentsSql, paymentArgs.toArray()));
        return result;
    }

    private void addGeoFilters(StringBuilder sql, List<Object> args, Long regionId, Long cityId, Long teamId, String cityAlias, String teamAlias) {
        if (regionId != null) { sql.append(" AND ").append(cityAlias).append(".region_id=?"); args.add(regionId); }
        if (cityId != null) { sql.append(" AND ").append(cityAlias).append(".id=?"); args.add(cityId); }
        if (teamId != null) { sql.append(" AND ").append(teamAlias).append(".id=?"); args.add(teamId); }
    }

    public List<UserRow> listUsers(LocalDate from, LocalDate to, Long regionId, Long cityId, Long teamId) {
        StringBuilder sql = new StringBuilder(userSelect() + " WHERE u.status<>'DELETED'");
        List<Object> args = new ArrayList<>();
        if (from != null) { sql.append(" AND (u.created_at AT TIME ZONE 'UTC')::date>=?"); args.add(from); }
        if (to != null) { sql.append(" AND (u.created_at AT TIME ZONE 'UTC')::date<=?"); args.add(to); }
        addGeoFilters(sql, args, regionId, cityId, teamId, "c", "t");
        sql.append(" GROUP BY u.id,p.user_id,c.id,r.id ORDER BY u.created_at DESC");
        return jdbc.query(sql.toString(), (rs, n) -> mapUser(rs), args.toArray());
    }

    private String userSelect() {
        return "SELECT u.*, NULLIF(BTRIM(CONCAT_WS(' ',p.last_name,p.first_name,p.middle_name)),'') full_name,c.id city_id,c.name city_name,r.id region_id,r.name region_name," +
                " string_agg(DISTINCT t.name,', ') FILTER (WHERE t.id IS NOT NULL) team_names," +
                " (SELECT string_agg(up.position,', ' ORDER BY up.position) FROM user_positions up WHERE up.user_id=u.id) position_names" +
                " FROM users u LEFT JOIN user_profiles p ON p.user_id=u.id LEFT JOIN cities c ON c.id=p.city_id LEFT JOIN regions r ON r.id=c.region_id" +
                " LEFT JOIN team_members tm ON tm.user_id=u.id AND tm.status='ACTIVE' LEFT JOIN teams t ON t.id=tm.team_id AND t.status<>'ARCHIVED'";
    }

    private UserRow mapUser(ResultSet rs) throws SQLException {
        return new UserRow(rs.getLong("id"), rs.getString("phone"), rs.getString("status"),
                rs.getBoolean("is_super_admin"), rs.getBoolean("is_ad_admin"), rs.getString("full_name"),
                nullableLong(rs, "city_id"), rs.getString("city_name"), nullableLong(rs, "region_id"), rs.getString("region_name"),
                split(rs.getString("team_names")), split(rs.getString("position_names")),
                rs.getObject("created_at", OffsetDateTime.class), rs.getString("block_reason"));
    }

    public UserDetails userDetails(Long id) {
        UserRow user = listUsers(null, null, null, null, null).stream().filter(row -> row.id().equals(id)).findFirst()
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        String sql = "SELECT t.id,t.name,tm.role,COALESCE(b.amount_kopecks,0) balance," +
                " (SELECT COUNT(*) FROM charges ch WHERE ch.team_id=t.id AND ch.user_id=? AND ch.status<>'CANCELLED') charge_count," +
                " (SELECT COALESCE(SUM(ch.amount_kopecks),0) FROM charges ch WHERE ch.team_id=t.id AND ch.user_id=? AND ch.status<>'CANCELLED') charge_sum," +
                " (SELECT COUNT(*) FROM payments p WHERE p.team_id=t.id AND p.user_id=? AND p.deleted_at IS NULL) payment_count," +
                " (SELECT COALESCE(SUM(p.amount_kopecks),0) FROM payments p WHERE p.team_id=t.id AND p.user_id=? AND p.deleted_at IS NULL) payment_sum" +
                " FROM team_members tm JOIN teams t ON t.id=tm.team_id LEFT JOIN balances b ON b.team_id=t.id AND b.user_id=tm.user_id WHERE tm.user_id=? ORDER BY t.name";
        List<TeamFinance> rows = jdbc.query(sql, (rs,n) -> new TeamFinance(rs.getLong("id"),rs.getString("name"),rs.getString("role"),rs.getLong("balance"),rs.getLong("charge_count"),rs.getLong("charge_sum"),rs.getLong("payment_count"),rs.getLong("payment_sum")), id,id,id,id,id);
        return new UserDetails(user, rows);
    }

    public List<TeamRow> listTeams(LocalDate from, LocalDate to, Long regionId, Long cityId, Long teamId) {
        StringBuilder sql = new StringBuilder(teamSelect() + " WHERE t.status<>'ARCHIVED'");
        List<Object> args = new ArrayList<>();
        if (from != null) { sql.append(" AND (t.created_at AT TIME ZONE 'UTC')::date>=?"); args.add(from); }
        if (to != null) { sql.append(" AND (t.created_at AT TIME ZONE 'UTC')::date<=?"); args.add(to); }
        addGeoFilters(sql, args, regionId, cityId, teamId, "c", "t");
        sql.append(" ORDER BY t.name");
        return jdbc.query(sql.toString(), (rs,n) -> mapTeam(rs), args.toArray());
    }

    private String teamSelect() {
        return "SELECT t.*,c.name city_name,r.id region_id,r.name region_name," +
                " (SELECT COUNT(*) FROM team_members tm WHERE tm.team_id=t.id AND tm.status='ACTIVE') members_count," +
                " (SELECT COUNT(*) FROM events e WHERE e.team_id=t.id) events_count," +
                " (SELECT COALESCE(SUM(ch.amount_kopecks),0) FROM charges ch WHERE ch.team_id=t.id AND ch.status<>'CANCELLED') charge_sum," +
                " (SELECT COALESCE(SUM(p.amount_kopecks),0) FROM payments p WHERE p.team_id=t.id AND p.deleted_at IS NULL) payment_sum" +
                " FROM teams t LEFT JOIN cities c ON c.id=t.city_id LEFT JOIN regions r ON r.id=t.home_region_id";
    }

    private TeamRow mapTeam(ResultSet rs) throws SQLException {
        return new TeamRow(rs.getLong("id"),rs.getString("name"),rs.getString("status"),nullableLong(rs,"city_id"),rs.getString("city_name"),nullableLong(rs,"region_id"),rs.getString("region_name"),rs.getObject("created_at",OffsetDateTime.class),rs.getString("block_reason"),rs.getLong("members_count"),rs.getLong("events_count"),rs.getLong("charge_sum"),rs.getLong("payment_sum"));
    }

    public TeamDetails teamDetails(Long id) {
        TeamRow team = listTeams(null,null,null,null,id).stream().findFirst().orElseThrow(() -> new NotFoundException("Команда не найдена"));
        String memberSql = "SELECT u.id,u.phone,NULLIF(BTRIM(CONCAT_WS(' ',p.last_name,p.first_name,p.middle_name)),u.phone) full_name,tm.role,tm.is_trainer,COALESCE(b.amount_kopecks,0) balance,(SELECT string_agg(up.position,', ' ORDER BY up.position) FROM user_positions up WHERE up.user_id=u.id) positions FROM team_members tm JOIN users u ON u.id=tm.user_id LEFT JOIN user_profiles p ON p.user_id=u.id LEFT JOIN balances b ON b.team_id=tm.team_id AND b.user_id=u.id WHERE tm.team_id=? AND tm.status='ACTIVE' ORDER BY full_name";
        List<MemberRow> memberRows = jdbc.query(memberSql,(rs,n)->new MemberRow(rs.getLong("id"),rs.getString("full_name"),rs.getString("phone"),rs.getString("role"),rs.getBoolean("is_trainer"),split(rs.getString("positions")),rs.getLong("balance")),id);
        List<EventRow> eventRows = jdbc.query("SELECT id,title,section,event_date,time_of_day,place,status FROM events WHERE team_id=? ORDER BY event_date DESC,time_of_day DESC LIMIT 100",(rs,n)->new EventRow(rs.getLong("id"),rs.getString("title"),rs.getString("section"),rs.getString("event_date"),rs.getString("time_of_day"),rs.getString("place"),rs.getString("status")),id);
        return new TeamDetails(team,memberRows,eventRows);
    }

    @Transactional
    public UserRow blockUser(Long targetId, Long actorId, String reason) {
        if (targetId.equals(actorId)) throw new ForbiddenException("Нельзя заблокировать самого себя");
        User user = users.findById(targetId).orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        if (user.getStatus() == UserStatus.DELETED) throw new ConflictException("Пользователь удалён");
        if (Boolean.TRUE.equals(user.getIsSuperAdmin())) throw new ForbiddenException("Нельзя заблокировать суперадмина");
        user.setStatus(UserStatus.BLOCKED); user.setBlockReason(reason.trim()); user.setBlockedAt(OffsetDateTime.now());
        users.save(user); refreshTokens.revokeAll(targetId);
        return userDetails(targetId).user();
    }

    @Transactional
    public UserRow unblockUser(Long targetId) {
        User user = users.findById(targetId).orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        if (user.getStatus() != UserStatus.BLOCKED) throw new ConflictException("Пользователь не заблокирован");
        user.setStatus(UserStatus.ACTIVE); user.setBlockReason(null); user.setBlockedAt(null); users.save(user);
        return userDetails(targetId).user();
    }

    @Transactional
    public UserRow setAdAdmin(Long targetId, boolean value) {
        User user = users.findById(targetId).orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        if (user.getStatus() == UserStatus.DELETED) throw new NotFoundException("Пользователь не найден");
        user.setIsAdAdmin(value); users.save(user); return userDetails(targetId).user();
    }

    @Transactional
    public TeamRow blockTeam(Long teamId, String reason, Long actorId) {
        Team team = teams.findById(teamId).orElseThrow(() -> new NotFoundException("Команда не найдена"));
        if (team.getStatus() == TeamStatus.ARCHIVED) throw new ConflictException("Команда удалена");
        team.setStatus(TeamStatus.BLOCKED); team.setBlockReason(reason.trim()); team.setBlockedAt(OffsetDateTime.now()); teams.save(team);
        members.findByTeamIdAndStatusOrderByJoinedAtAsc(teamId, MemberStatus.ACTIVE).forEach(member ->
                notifications.send(member.getUserId(), NotificationType.ADMIN_MESSAGE, "Команда заблокирована", "«" + team.getName() + "»: " + reason.trim() + ". Доступен только просмотр истории.", teamId, Map.of("teamId",teamId,"blocked",true)));
        return teamDetails(teamId).team();
    }

    @Transactional
    public TeamRow unblockTeam(Long teamId) {
        Team team = teams.findById(teamId).orElseThrow(() -> new NotFoundException("Команда не найдена"));
        if (team.getStatus() != TeamStatus.BLOCKED) throw new ConflictException("Команда не заблокирована");
        team.setStatus(TeamStatus.ACTIVE); team.setBlockReason(null); team.setBlockedAt(null); teams.save(team);
        return teamDetails(teamId).team();
    }

    public List<TargetOption> adTargetTeams() {
        return jdbc.query("SELECT id,name,home_region_id,city_id FROM teams WHERE status<>'ARCHIVED' ORDER BY name",
                (rs,n) -> new TargetOption(rs.getLong("id"),rs.getString("name"),
                        nullableLong(rs,"home_region_id"),nullableLong(rs,"city_id")));
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException { long value=rs.getLong(column); return rs.wasNull()?null:value; }
    private static List<String> split(String value) { return value==null||value.isBlank()?List.of():Arrays.stream(value.split(", ")).filter(s->!s.isBlank()).toList(); }
    private static long number(Object value) { return value instanceof Number n?n.longValue():0L; }
}
