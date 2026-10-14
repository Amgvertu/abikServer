package info.prorabka.abik.server.team.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Запрос на передачу владения командой.
 * Необратимая операция.
 */
public record TransferOwnershipRequest(

        @NotNull(message = "Укажите пользователя, которому передаётся владение")
        Long newOwnerUserId
) {}