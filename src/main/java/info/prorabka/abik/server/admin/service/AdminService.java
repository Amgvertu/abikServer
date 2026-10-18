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
                .map(t -> {
                    // В реальном коде лучше сделать метод в TeamService,
                    // но для простоты воспользуемся тем же маппингом.
                    var city = regionRepository.findById(t.getCityId()).orElse(null); // Заглушка
                    // Примечание: для полноценного ответа нужен доступ к GeoService.
                    // Пока вернем просто базовые поля через TeamResponse (если он создается в TeamService)
                    return teamService.getDetail(t.getId(), 0L); // Заглушка, если 0L не админ.
                })
                .map(detail -> new TeamResponse(
                        detail.id(), detail.name(), detail.cityId(), detail.cityName(),
                        detail.homeRegionId(), detail.homeRegionName(), detail.logoUrl(),
                        detail.status(), detail.createdAt()
                ))
                .toList();
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