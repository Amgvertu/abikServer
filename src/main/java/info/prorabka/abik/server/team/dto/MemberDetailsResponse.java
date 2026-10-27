package info.prorabka.abik.server.team.dto;

import info.prorabka.abik.server.team.entity.MemberRole;
import info.prorabka.abik.server.team.entity.MemberStatus;
import info.prorabka.abik.server.user.dto.UserProfileResponse;

import java.time.OffsetDateTime;

/**
 * Team-scoped карточка участника (ТЗ 9.2, BE-07).
 * Данные членства (роль, номер, тренер, статус) + телефон + профиль.
 * profile = null, если участник ещё не заполнил профиль.
 */
public record MemberDetailsResponse(
        Long userId,
        String phone,
        MemberRole role,
        MemberStatus membershipStatus,
        Short jerseyNumber,
        Boolean isTrainer,
        OffsetDateTime joinedAt,
        UserProfileResponse profile
) {}