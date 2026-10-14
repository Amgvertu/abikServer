package info.prorabka.abik.server.team.service;

import info.prorabka.abik.server.common.exception.BadRequestException;
import info.prorabka.abik.server.common.exception.ForbiddenException;
import info.prorabka.abik.server.common.exception.NotFoundException;
import info.prorabka.abik.server.geo.entity.City;
import info.prorabka.abik.server.geo.entity.Region;
import info.prorabka.abik.server.geo.service.GeoService;
import info.prorabka.abik.server.team.dto.TeamCreateRequest;
import info.prorabka.abik.server.team.dto.TeamDetailResponse;
import info.prorabka.abik.server.team.dto.TeamResponse;
import info.prorabka.abik.server.team.dto.TeamUpdateRequest;
import info.prorabka.abik.server.team.entity.*;
import info.prorabka.abik.server.team.repository.OwnershipTransferRepository;
import info.prorabka.abik.server.team.repository.TeamMemberRepository;
import info.prorabka.abik.server.team.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Сервис команд: создание, чтение, обновление, архивация, передача владения.
 */
@Service
public class TeamService {

    private static final Logger log = LoggerFactory.getLogger(TeamService.class);

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final OwnershipTransferRepository ownershipTransferRepository;
    private final TeamMemberService teamMemberService;
    private final GeoService geoService;

    public TeamService(TeamRepository teamRepository,
                       TeamMemberRepository teamMemberRepository,
                       OwnershipTransferRepository ownershipTransferRepository,
                       TeamMemberService teamMemberService,
                       GeoService geoService) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.ownershipTransferRepository = ownershipTransferRepository;
        this.teamMemberService = teamMemberService;
        this.geoService = geoService;
    }

    // ============================================================
    // Создание
    // ============================================================

    @Transactional
    public TeamDetailResponse create(Long creatorUserId, TeamCreateRequest req) {
        // Проверяем город и регион
        City city = geoService.getCity(req.cityId());
        Region region = geoService.getRegion(req.homeRegionId());

        Team team = Team.builder()
                .name(req.name().trim())
                .cityId(city.getId())
                .homeRegionId(region.getId())
                .logoUrl(req.logoUrl().trim())
                .description(trimOrNull(req.description()))
                .season(trimOrNull(req.season()))
                .status(TeamStatus.ACTIVE)
                .build();

        Team saved = teamRepository.save(team);

        // Создатель становится владельцем
        teamMemberService.addMember(saved.getId(), creatorUserId, MemberRole.OWNER);

        log.info("Команда создана: teamId={}, ownerUserId={}", saved.getId(), creatorUserId);

        return buildDetail(saved, creatorUserId);
    }

    // ============================================================
    // Чтение
    // ============================================================

    /** Список команд, в которых пользователь — активный участник. */
    public List<TeamResponse> listMyTeams(Long userId) {
        List<Long> teamIds = teamMemberRepository
                .findByUserIdAndStatusOrderByJoinedAtDesc(userId, MemberStatus.ACTIVE)
                .stream()
                .map(TeamMember::getTeamId)
                .toList();

        return teamRepository.findAllById(teamIds).stream()
                .map(this::buildShort)
                .toList();
    }

    /** Детали команды. Доступно только активным участникам. */
    public TeamDetailResponse getDetail(Long teamId, Long userId) {
        Team team = getTeamOrThrow(teamId);
        teamMemberService.requireMember(teamId, userId);
        return buildDetail(team, userId);
    }

    // ============================================================
    // Обновление
    // ============================================================

    @Transactional
    public TeamDetailResponse update(Long teamId, Long actorUserId, TeamUpdateRequest req) {
        teamMemberService.requireManager(teamId, actorUserId);

        Team team = getTeamOrThrow(teamId);
        if (team.getStatus() == TeamStatus.ARCHIVED) {
            throw new ForbiddenException("Команда в архиве, изменения невозможны");
        }

        City city = geoService.getCity(req.cityId());
        Region region = geoService.getRegion(req.homeRegionId());

        team.setName(req.name().trim());
        team.setCityId(city.getId());
        team.setHomeRegionId(region.getId());
        team.setLogoUrl(req.logoUrl().trim());
        team.setDescription(trimOrNull(req.description()));
        team.setSeason(trimOrNull(req.season()));

        Team saved = teamRepository.save(team);
        log.info("Команда обновлена: teamId={}, actorUserId={}", teamId, actorUserId);

        return buildDetail(saved, actorUserId);
    }

    // ============================================================
    // Архивация
    // ============================================================

    @Transactional
    public TeamDetailResponse archive(Long teamId, Long actorUserId) {
        teamMemberService.requireOwner(teamId, actorUserId);

        Team team = getTeamOrThrow(teamId);
        if (team.getStatus() == TeamStatus.ARCHIVED) {
            throw new BadRequestException("Команда уже в архиве");
        }

        team.setStatus(TeamStatus.ARCHIVED);
        team.setArchivedAt(OffsetDateTime.now());
        Team saved = teamRepository.save(team);

        log.info("Команда архивирована: teamId={}, actorUserId={}", teamId, actorUserId);
        return buildDetail(saved, actorUserId);
    }

    // ============================================================
    // Передача владения
    // ============================================================

    @Transactional
    public TeamDetailResponse transferOwnership(Long teamId, Long actorUserId, Long newOwnerUserId) {
        teamMemberService.requireOwner(teamId, actorUserId);

        if (actorUserId.equals(newOwnerUserId)) {
            throw new BadRequestException("Вы уже владелец этой команды");
        }

        // Новый владелец должен быть активным участником
        TeamMember newOwner = teamMemberService.requireMember(teamId, newOwnerUserId);

        // Текущий владелец
        TeamMember currentOwner = teamMemberService.requireOwner(teamId, actorUserId);

        // Меняем роли
        currentOwner.setRole(MemberRole.MODERATOR);   // бывший владелец становится модератором
        newOwner.setRole(MemberRole.OWNER);
        teamMemberRepository.save(currentOwner);
        teamMemberRepository.save(newOwner);

        // Запись в историю
        ownershipTransferRepository.save(OwnershipTransfer.builder()
                .teamId(teamId)
                .fromUserId(actorUserId)
                .toUserId(newOwnerUserId)
                .transferredAt(OffsetDateTime.now())
                .build());

        log.info("Владение передано: teamId={}, from={}, to={}", teamId, actorUserId, newOwnerUserId);

        return buildDetail(getTeamOrThrow(teamId), actorUserId);
    }

    /** История передач владения. Доступно активным участникам. */
    public List<OwnershipTransfer> getTransferHistory(Long teamId, Long userId) {
        teamMemberService.requireMember(teamId, userId);
        return ownershipTransferRepository.findByTeamIdOrderByTransferredAtDesc(teamId);
    }

    // ============================================================
    // Helpers
    // ============================================================

    public Team getTeamOrThrow(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Команда не найдена"));
    }

    private TeamResponse buildShort(Team team) {
        City city = geoService.getCity(team.getCityId());
        Region region = geoService.getRegion(team.getHomeRegionId());
        return TeamResponse.from(team, city.getName(), region.getName());
    }

    private TeamDetailResponse buildDetail(Team team, Long actorUserId) {
        City city = geoService.getCity(team.getCityId());
        Region region = geoService.getRegion(team.getHomeRegionId());
        MemberRole myRole = teamMemberService.getRoleOrNull(team.getId(), actorUserId);
        long membersCount = teamMemberService.countActive(team.getId());
        return TeamDetailResponse.from(team, city.getName(), region.getName(), myRole, membersCount);
    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}