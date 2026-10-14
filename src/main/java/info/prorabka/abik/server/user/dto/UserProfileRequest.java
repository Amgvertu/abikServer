package info.prorabka.abik.server.user.dto;

import info.prorabka.abik.server.user.entity.StickHand;
import info.prorabka.abik.server.user.entity.UserPosition;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.Set;

/**
 * Запрос на создание или обновление профиля.
 */
public record UserProfileRequest(

        @NotNull(message = "Город обязателен")
        Long cityId,

        @NotBlank(message = "Фамилия обязательна")
        @Size(max = 100)
        String lastName,

        @NotBlank(message = "Имя обязательно")
        @Size(max = 100)
        String firstName,

        @NotBlank(message = "Отчество обязательно")
        @Size(max = 100)
        String middleName,

        @NotNull(message = "Дата рождения обязательна")
        @Past(message = "Дата рождения должна быть в прошлом")
        LocalDate birthDate,

        @NotNull(message = "Рост обязателен")
        @Min(value = 100, message = "Рост минимум 100 см")
        @Max(value = 250, message = "Рост максимум 250 см")
        Short heightCm,

        @NotNull(message = "Вес обязателен")
        @Min(value = 30, message = "Вес минимум 30 кг")
        @Max(value = 200, message = "Вес максимум 200 кг")
        Short weightKg,

        @NotNull(message = "Хват клюшки обязателен")
        StickHand stickHand,

        @NotEmpty(message = "Укажите хотя бы одно амплуа")
        Set<UserPosition> positions,

        @Size(max = 20)
        String jerseySize,

        @Size(max = 20)
        String coversSize,

        @Size(max = 20)
        String gaitersSize
) {}
