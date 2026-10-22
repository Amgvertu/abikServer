package info.prorabka.abik.server.user.dto;

import info.prorabka.abik.server.user.entity.StickHand;
import info.prorabka.abik.server.user.entity.UserPosition;
import info.prorabka.abik.server.user.entity.UserProfile;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;

/**
 * Полное представление профиля для клиента.
 */
public record UserProfileResponse(
        Long userId,
        Long cityId,
        String cityName,
        Long regionId,
        String regionName,
        String lastName,
        String firstName,
        String middleName,
        LocalDate birthDate,
        Short heightCm,
        Short weightKg,
        StickHand stickHand,
        Set<UserPosition> positions,
        String jerseySize,
        String coversSize,
        String gaitersSize,
        String avatarUrl,
        OffsetDateTime updatedAt
) {
    public static UserProfileResponse from(UserProfile p, String cityName, Long regionId, String regionName) {
        return new UserProfileResponse(
                p.getUserId(),
                p.getCityId(),
                cityName,
                regionId,
                regionName,
                p.getLastName(),
                p.getFirstName(),
                p.getMiddleName(),
                p.getBirthDate(),
                p.getHeightCm(),
                p.getWeightKg(),
                p.getStickHand(),
                p.getPositions(),
                p.getJerseySize(),
                p.getCoversSize(),
                p.getGaitersSize(),
                p.getAvatarUrl(),
                p.getUpdatedAt()
        );
    }
}