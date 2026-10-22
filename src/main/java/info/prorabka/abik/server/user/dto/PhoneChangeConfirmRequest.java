package info.prorabka.abik.server.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneChangeConfirmRequest(
        @NotBlank String changeToken,
        @NotBlank @Pattern(regexp = "^[0-9]{4}$") String code
) {}