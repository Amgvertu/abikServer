package info.prorabka.abik.server.admin.service;

import info.prorabka.abik.server.admin.dto.AdminUserResponse;
import info.prorabka.abik.server.admin.dto.CreateCityRequest;
import info.prorabka.abik.server.admin.dto.CreateRegionRequest;
import info.prorabka.abik.server.common.exception.ConflictException;
import info.prorabka.abik.server.common.exception.ForbiddenException;
import info.prorabka.abik.server.common.exception.NotFoundException;
import info.prorabka.abik.server.geo.entity.City;
import info.prorabka.abik.server.geo.entity.Region;
import info.prorabka.abik.server.geo.repository.CityRepository;
import info.prorabka.abik.server.geo.repository.RegionRepository;
import info.prorabka.abik.server.team.dto.TeamResponse;
import info.prorabka.abik.server.team.entity.TeamStatus;
import info.prorabka.abik.server.team.repository.TeamRepository;
import info.prorabka.abik.server.team.service.TeamService;
import info.prorabka.abik.server.user.entity.User;
import info.prorabka.abik.server.user.entity.UserStatus;
import info.prorabka.abik.server.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository userRepository;
    private final RegionRepository regionRepository;
    private final CityRepository cityRepository;
    private final TeamRepository teamRepository;
    private final TeamService teamService; // Используем для маппинга команд

    public AdminService(UserRepository userRepository,
                        RegionRepository regionRepository,
                        CityRepository cityRepository,
                        TeamRepository teamRepository,
                        TeamService teamService) {
        this.userRepository = userRepository;
        this.regionRepository = regionRepository;
        this.cityRepository = cityRepository;
        this.teamRepository = teamRepository;
        this.teamService = teamService;
    }

    // ============================================================
    // Пользователи
    // ============================================================

    public List<AdminUserResponse> listUsers() {
        return userRepository.findAll().stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    @Transactional
    public AdminUserResponse blockUser(Long targetUserId, Long actorUserId) {
        if (targetUserId.equals(actorUserId)) {
            throw new ForbiddenException("Нельзя заблокировать самого себя");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        if (user.getStatus() == UserStatus.DELETED) {
            throw new ConflictException("Пользователь уже удалён");
        }

        user.setStatus(UserStatus.BLOCKED);
        User saved = userRepository.save(user);
        log.info("Суперадмин {} заблокировал пользователя {}", actorUserId, targetUserId);
        return AdminUserResponse.from(saved);
    }

    @Transactional
    public AdminUserResponse unblockUser(Long targetUserId, Long actorUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        if (user.getStatus() != UserStatus.BLOCKED) {
            throw new ConflictException("Пользователь не заблокирован");
        }

        user.setStatus(UserStatus.ACTIVE);
        User saved = userRepository.save(user);
        log.info("Суперадмин {} разблокировал пользователя {}", actorUserId, targetUserId);
        return AdminUserResponse.from(saved);
    }

    // ============================================================
    // Команды
    // ============================================================

    public List<TeamResponse> listAllTeams() {
        return teamRepository.findAllByOrderByNameAsc().stream()
                .map(team -> {
                    String cityName = cityRepository.findById(team.getCityId())
                            .map(City::getName).orElse(null);
                    String regionName = regionRepository.findById(team.getHomeRegionId())
                            .map(Region::getName).orElse(null);
                    return TeamResponse.from(team, cityName, regionName);
                })
                .toList();
    }

    public info.prorabka.abik.server.admin.dto.AdminStatsResponse getStats() {
        var users = userRepository.findAll();
        var teams = teamRepository.findAll();
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate week = today.minusDays(6);
        java.time.LocalDate month = today.minusDays(29);
        java.util.List<info.prorabka.abik.server.admin.dto.AdminStatsResponse.DailyStat> daily = new java.util.ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            java.time.LocalDate day = today.minusDays(i);
            long userCount = users.stream().filter(u -> u.getCreatedAt() != null && u.getCreatedAt().toLocalDate().equals(day)).count();
            long teamCount = teams.stream().filter(t -> t.getCreatedAt() != null && t.getCreatedAt().toLocalDate().equals(day)).count();
            daily.add(new info.prorabka.abik.server.admin.dto.AdminStatsResponse.DailyStat(day, userCount, teamCount));
        }
        return new info.prorabka.abik.server.admin.dto.AdminStatsResponse(
                users.size(), teams.size(),
                users.stream().filter(u -> u.getCreatedAt() != null && !u.getCreatedAt().toLocalDate().isBefore(today)).count(),
                users.stream().filter(u -> u.getCreatedAt() != null && !u.getCreatedAt().toLocalDate().isBefore(week)).count(),
                users.stream().filter(u -> u.getCreatedAt() != null && !u.getCreatedAt().toLocalDate().isBefore(month)).count(),
                teams.stream().filter(t -> t.getCreatedAt() != null && !t.getCreatedAt().toLocalDate().isBefore(today)).count(),
                teams.stream().filter(t -> t.getCreatedAt() != null && !t.getCreatedAt().toLocalDate().isBefore(week)).count(),
                teams.stream().filter(t -> t.getCreatedAt() != null && !t.getCreatedAt().toLocalDate().isBefore(month)).count(), daily);
    }

    // ============================================================
    // Справочники
    // ============================================================

    @Transactional
    public Region createRegion(CreateRegionRequest req) {
        if (regionRepository.findByCode(req.code()).isPresent()) {
            throw new ConflictException("Регион с таким кодом уже существует");
        }
        Region region = Region.builder()
                .name(req.name().trim())
                .code(req.code().trim())
                .timezone(req.timezone().trim())
                .build();
        return regionRepository.save(region);
    }

    @Transactional
    public City createCity(CreateCityRequest req) {
        Region region = regionRepository.findById(req.regionId())
                .orElseThrow(() -> new NotFoundException("Регион не найден"));

        City city = City.builder()
                .regionId(region.getId())
                .name(req.name().trim())
                .build();
        return cityRepository.save(city);
    }
}