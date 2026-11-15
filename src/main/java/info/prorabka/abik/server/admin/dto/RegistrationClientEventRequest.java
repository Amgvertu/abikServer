package info.prorabka.abik.server.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationClientEventRequest(
        @NotBlank @Size(max = 100) String requestId,
        @Size(max = 20) String phone,
        @NotBlank @Size(max = 50) String stage,
        @NotBlank @Size(max = 30) String status,
        @Size(max = 80) String code,
        @Size(max = 1000) String details,
        @Size(max = 30) String client
) {}
