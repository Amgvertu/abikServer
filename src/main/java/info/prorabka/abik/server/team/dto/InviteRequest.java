package info.prorabka.abik.server.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Запрос на приглашение в команду по номеру телефона.
 */
public record InviteRequest(

        @NotBlank(message = "Телефон обязателен")
        @Pattern(regexp = "^\\+?[0-9]{10,15}$",
                message = "Телефон в формате E.164 или 10-15 цифр")
        String phone
) {}