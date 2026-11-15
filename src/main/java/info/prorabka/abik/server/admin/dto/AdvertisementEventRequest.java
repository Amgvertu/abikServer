package info.prorabka.abik.server.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdvertisementEventRequest(
        @NotBlank @Size(max = 100) String sessionId,
        @NotBlank @Size(max = 10) String type,
        Long teamId
) {}
