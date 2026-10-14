package info.prorabka.abik.server.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Запрос на подтверждение регистрации кодом из SMS.
 */
public record RegisterConfirmRequest(

        @NotBlank(message = "Телефон обязателен")
        @Pattern(regexp = "^\\+?[0-9]{10,15}$")
        String phone,

        @NotBlank(message = "Код обязателен")
        @Pattern(regexp = "^[0-9]{4}$", message = "Код — 4 цифры")
        String code
) {}
