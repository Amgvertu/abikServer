package info.prorabka.abik.server.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BlockRequest(
        @NotBlank(message = "Укажите причину блокировки")
        @Size(max = 500) String reason
) {}
