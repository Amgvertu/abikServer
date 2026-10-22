package info.prorabka.abik.server.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PasswordResetStartRequest(
        @NotBlank
        @Pattern(regexp = "^\\+?[0-9]{10,15}$")
        String phone
) {}