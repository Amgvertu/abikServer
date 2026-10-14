package info.prorabka.abik.server.user.dto;

import info.prorabka.abik.server.user.entity.User;
import info.prorabka.abik.server.user.entity.UserStatus;

import java.time.OffsetDateTime;

/**
 * Публичное представление пользователя для клиента.
 * НЕ содержит passwordHash и других внутренних полей.
 */
public record UserResponse(
        Long id,
        String phone,
        UserStatus status,
        OffsetDateTime createdAt
) {
    /** Преобразовать сущность в DTO. */
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getPhone(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}
