package info.prorabka.abik.server.user.service;

import info.prorabka.abik.server.common.exception.BadRequestException;
import info.prorabka.abik.server.common.exception.NotFoundException;
import info.prorabka.abik.server.geo.entity.City;
import info.prorabka.abik.server.geo.entity.Region;
import info.prorabka.abik.server.geo.service.GeoService;
import info.prorabka.abik.server.user.dto.UserProfileRequest;
import info.prorabka.abik.server.user.dto.UserProfileResponse;
import info.prorabka.abik.server.user.entity.UserProfile;
import info.prorabka.abik.server.user.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

/**
 * Сервис профиля пользователя.
 */
@Service
public class UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);

    private final UserProfileRepository userProfileRepository;
    private final GeoService geoService;

    public UserProfileService(UserProfileRepository userProfileRepository, GeoService geoService) {
        this.userProfileRepository = userProfileRepository;
        this.geoService = geoService;
    }

    /**
     * Создать или обновить профиль пользователя.
     * Одна точка входа и для создания, и для редактирования — PUT-семантика.
     */
    @Transactional
    public UserProfileResponse createOrUpdate(Long userId, UserProfileRequest req) {
        // 1. Проверяем город
        City city = geoService.getCity(req.cityId());

        // 2. Ищем существующий профиль или создаём новый
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseGet(() -> UserProfile.builder().userId(userId).build());

        // 3. Заполняем поля
        profile.setCityId(city.getId());
        profile.setLastName(req.lastName().trim());
        profile.setFirstName(req.firstName().trim());
        profile.setMiddleName(req.middleName().trim());
        profile.setBirthDate(req.birthDate());
        profile.setHeightCm(req.heightCm());
        profile.setWeightKg(req.weightKg());
        profile.setStickHand(req.stickHand());
        profile.setJerseySize(trimOrNull(req.jerseySize()));
        profile.setCoversSize(trimOrNull(req.coversSize()));
        profile.setGaitersSize(trimOrNull(req.gaitersSize()));

        // 4. Заменяем амплуа целиком
        profile.setPositions(new HashSet<>(req.positions()));

        // 5. Сохраняем
        UserProfile saved = userProfileRepository.save(profile);
        log.info("Профиль сохранён: userId={}", userId);

        return buildResponse(saved);
    }

    /** Получить профиль по userId. */
    public UserProfileResponse getByUserId(Long userId) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Профиль не найден"));
        return buildResponse(profile);
    }

    /** Проверить существование профиля. */
    public boolean exists(Long userId) {
        return userProfileRepository.existsByUserId(userId);
    }

    // ============================================================
    // Helpers
    // ============================================================

    /** Собрать UserProfileResponse с подтянутыми именем города и региона. */
    private UserProfileResponse buildResponse(UserProfile p) {
        City city = geoService.getCity(p.getCityId());
        Region region = geoService.getRegion(city.getRegionId());

        return UserProfileResponse.from(p, city.getName(), region.getId(), region.getName());
    }

    /** trim(), но возвращает null для пустой строки. */
    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}