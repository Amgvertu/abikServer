package info.prorabka.abik.server.roster.service;

import info.prorabka.abik.server.charge.dto.ChargeCreateManualRequest;
import info.prorabka.abik.server.charge.service.ChargeService;
import info.prorabka.abik.server.common.exception.NotFoundException;
import info.prorabka.abik.server.roster.dto.*;
import info.prorabka.abik.server.roster.entity.ChampionshipRoster;
import info.prorabka.abik.server.roster.repository.ChampionshipRosterRepository;
import info.prorabka.abik.server.team.entity.MemberRole;
import info.prorabka.abik.server.team.entity.Team;
import info.prorabka.abik.server.team.entity.TeamMember;
import info.prorabka.abik.server.team.repository.TeamRepository;
import info.prorabka.abik.server.team.service.TeamMemberService;
import info.prorabka.abik.server.user.entity.UserProfile;
import info.prorabka.abik.server.user.entity.UserPosition;
import info.prorabka.abik.server.user.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class RosterService {

    private static final Logger log = LoggerFactory.getLogger(RosterService.class);

    private final ChampionshipRosterRepository repo;
    private final TeamMemberService teamMemberService;
    private final TeamRepository teamRepo;
    private final UserProfileRepository profileRepo;
    private final ChargeService chargeService;

    public RosterService(ChampionshipRosterRepository repo,
                         TeamMemberService teamMemberService,
                         TeamRepository teamRepo,
                         UserProfileRepository profileRepo,
                         ChargeService chargeService) {
        this.repo = repo;
        this.teamMemberService = teamMemberService;
        this.teamRepo = teamRepo;
        this.profileRepo = profileRepo;
        this.chargeService = chargeService;
    }

    public List<SeasonResponse> listSeasons(Long teamId, Long actor) {
        teamMemberService.requireMember(teamId, actor);
        Set<String> seasons = new LinkedHashSet<>(repo.findDistinctSeasonsByTeamId(teamId));
        String current = teamRepo.findById(teamId).map(Team::getSeason).orElse(null);
        if (current != null && !current.isBlank()) seasons.add(current);
        // Стабильная сортировка: по убыванию (новые сезоны первыми)
        List<String> sorted = new ArrayList<>(seasons);
        sorted.sort(Comparator.reverseOrder());
        List<SeasonResponse> result = new ArrayList<>();
        for (String s : sorted) {
            result.add(new SeasonResponse(s, s.equals(current)));
        }
        return result;
    }

    /**
     * Архивный сезон защищён от изменений.
     * Текущий сезон = поле season команды. Если у команды сезон не заполнен,
     * запись разрешаем (иначе нельзя завести первый состав).
     */
    private void requireWritableSeason(Long teamId, String season) {
        String current = teamRepo.findById(teamId).map(Team::getSeason).orElse(null);
        if (current != null && !current.isBlank() && !current.equals(season)) {
            throw new info.prorabka.abik.server.common.exception.BadRequestException(
                    "Сезон " + season + " архивный: изменения запрещены. Текущий сезон: " + current);
        }
    }

    /**
     * Проверить, что пользователь — активный участник команды.
     * Иначе — 400 (не 403: ошибка в данных запроса, а не в правах актора).
     */
    private void requireActiveMemberOfTeam(Long teamId, Long userId) {
        try {
            teamMemberService.requireMember(teamId, userId);
        } catch (RuntimeException e) {
            throw new info.prorabka.abik.server.common.exception.BadRequestException(
                    "Пользователь не является активным участником команды: userId=" + userId);
        }
    }

    public List<RosterItemResponse> listRoster(Long teamId, String season, Long actor) {
        teamMemberService.requireMember(teamId, actor);
        List<ChampionshipRoster> list = repo
                .findByTeamIdAndSeasonOrderByAddedAtAsc(teamId, season);
        List<RosterItemResponse> result = new ArrayList<>();
        for (ChampionshipRoster r : list) {
            String fullName = profileRepo.findById(r.getUserId())
                    .map(p -> p.getLastName() + " " + p.getFirstName())
                    .orElse("user#" + r.getUserId());
            Set<UserPosition> positions = profileRepo.findById(r.getUserId())
                    .map(p -> (Set<UserPosition>) new HashSet<>(p.getPositions()))
                    .orElse(Set.of());
            result.add(new RosterItemResponse(
                    r.getUserId(), fullName, positions, r.getStatus(), r.getAddedAt()));
        }
        return result;
    }

    @Transactional
    public void replaceRoster(Long teamId, String season, Long actor,
                              RosterReplaceRequest req) {
        TeamMember m = teamMemberService.requireManager(teamId, actor);
        if (m.getRole() != MemberRole.OWNER && m.getRole() != MemberRole.ADMIN) {
            throw new info.prorabka.abik.server.common.exception.ForbiddenException(
                    "Изменять состав может только владелец или админ");
        }
        requireWritableSeason(teamId, season);
        // В состав можно добавлять только активных участников команды
        for (Long uid : req.userIds()) {
            requireActiveMemberOfTeam(teamId, uid);
        }

        List<ChampionshipRoster> existing = repo
                .findByTeamIdAndSeasonOrderByAddedAtAsc(teamId, season);
        Map<Long, ChampionshipRoster> byUser = new HashMap<>();
        for (ChampionshipRoster r : existing) byUser.put(r.getUserId(), r);

        // Новые
        for (Long uid : req.userIds()) {
            ChampionshipRoster r = byUser.get(uid);
            if (r == null) {
                repo.save(ChampionshipRoster.builder()
                        .teamId(teamId).season(season).userId(uid)
                        .status("ACTIVE").build());
            } else if (!"ACTIVE".equals(r.getStatus())) {
                r.setStatus("ACTIVE");
                r.setRemovedAt(null);
                r.setAddedAt(OffsetDateTime.now());
                repo.save(r);
            }
        }
        // Убранные
        for (ChampionshipRoster r : existing) {
            if ("ACTIVE".equals(r.getStatus()) && !req.userIds().contains(r.getUserId())) {
                r.setStatus("REMOVED");
                r.setRemovedAt(OffsetDateTime.now());
                repo.save(r);
            }
        }
    }

    @Transactional
    public RosterItemResponse addMember(Long teamId, String season, Long actor,
                                        RosterAddRequest req) {
        TeamMember m = teamMemberService.requireManager(teamId, actor);
        if (m.getRole() != MemberRole.OWNER && m.getRole() != MemberRole.ADMIN) {
            throw new info.prorabka.abik.server.common.exception.ForbiddenException(
                    "Добавлять игроков может только владелец или админ");
        }
        requireWritableSeason(teamId, season);
        requireActiveMemberOfTeam(teamId, req.userId());

        ChampionshipRoster existing = repo
                .findByTeamIdAndSeasonAndUserId(teamId, season, req.userId())
                .orElse(null);

        // Идемпотентность: игрок уже активен в составе — ничего не меняем,
        // повторный стартовый взнос НЕ создаём.
        if (existing != null && "ACTIVE".equals(existing.getStatus())) {
            log.info("Игрок уже в составе, повторное добавление проигнорировано: "
                    + "teamId={}, season={}, userId={}", teamId, season, req.userId());
            return listRoster(teamId, season, actor).stream()
                    .filter(i -> i.userId().equals(req.userId()))
                    .findFirst().orElseThrow(() -> new NotFoundException("Не найден в составе"));
        }

        ChampionshipRoster r = existing != null ? existing
                : ChampionshipRoster.builder()
                .teamId(teamId).season(season).userId(req.userId()).build();
        r.setStatus("ACTIVE");
        r.setRemovedAt(null);
        r.setAddedAt(OffsetDateTime.now());
        repo.save(r);

        if (Boolean.TRUE.equals(req.chargeEntryFee())) {
            if (req.entryFee() == null) {
                throw new info.prorabka.abik.server.common.exception.BadRequestException(
                        "entryFee обязателен при chargeEntryFee=true");
            }
            EntryFeeDto fee = req.entryFee();
            ChargeCreateManualRequest chargeReq = new ChargeCreateManualRequest(
                    fee.groupId(), fee.subgroupId(),
                    Set.of(req.userId()), fee.amountKopecks(),
                    "Стартовый взнос за сезон " + season,
                    fee.dueDate());
            chargeService.createManual(teamId, actor, chargeReq);
        }

        return listRoster(teamId, season, actor).stream()
                .filter(i -> i.userId().equals(req.userId()))
                .findFirst().orElseThrow(() -> new NotFoundException("Не найден в составе"));
    }

    @Transactional
    public void removeMember(Long teamId, String season, Long userId, Long actor) {
        TeamMember m = teamMemberService.requireManager(teamId, actor);
        if (m.getRole() != MemberRole.OWNER && m.getRole() != MemberRole.ADMIN) {
            throw new info.prorabka.abik.server.common.exception.ForbiddenException(
                    "Убирать игроков может только владелец или админ");
        }
        requireWritableSeason(teamId, season);
        ChampionshipRoster r = repo.findByTeamIdAndSeasonAndUserId(teamId, season, userId)
                .orElseThrow(() -> new NotFoundException("Игрок не в составе"));
        r.setStatus("REMOVED");
        r.setRemovedAt(OffsetDateTime.now());
        repo.save(r);
    }
}