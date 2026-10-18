package info.prorabka.abik.server.admin.dto;

import info.prorabka.abik.server.user.entity.User;
import info.prorabka.abik.server.user.entity.UserStatus;

import java.time.OffsetDateTime;

public record AdminUserResponse(
        Long id,
        String phone,
        UserStatus status,
        Boolean isSuperAdmin,
        OffsetDateTime createdAt
) {
    public static AdminUserResponse from(User u) {
        return new AdminUserResponse(
                u.getId(),
                u.getPhone(),
                u.getStatus(),
                u.getIsSuperAdmin(),
                u.getCreatedAt()
        );
    }
}