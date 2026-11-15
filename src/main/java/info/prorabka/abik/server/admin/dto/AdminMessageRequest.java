package info.prorabka.abik.server.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminMessageRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 1000) String body,
        @Size(max = 2000) String linkUrl,
        Long regionId,
        Long cityId,
        Long teamId,
        @Size(max = 30) String position
) {}
