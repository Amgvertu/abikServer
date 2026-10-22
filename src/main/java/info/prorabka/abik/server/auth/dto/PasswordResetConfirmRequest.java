package info.prorabka.abik.server.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
        @NotBlank @Pattern(regexp = "^\\+?[0-9]{10,15}$") String phone,
        @NotBlank @Pattern(regexp = "^[0-9]{4}$") String code,
        @NotBlank @Size(min = 8, max = 100) String newPassword
) {}