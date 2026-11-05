package info.prorabka.abik.server.offline.dto;

import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OfflineOperationRequest(
        @NotNull UUID operationId,
        @NotBlank String type,
        @NotNull JsonNode payload,
        Long expectedEventRevision
) {}
