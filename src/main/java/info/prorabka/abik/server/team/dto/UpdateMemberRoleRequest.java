package info.prorabka.abik.server.team.dto;

import info.prorabka.abik.server.team.entity.MemberRole;
import jakarta.validation.constraints.NotNull;

/**
 * Запрос на изменение роли участника.
 * Владельца через этот метод не назначаем — для этого есть transferOwnership.
 */
public record UpdateMemberRoleRequest(

        @NotNull(message = "Роль обязательна")
        MemberRole role
) {}