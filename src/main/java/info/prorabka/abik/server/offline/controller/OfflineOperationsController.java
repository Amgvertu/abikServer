package info.prorabka.abik.server.offline.controller;

import tools.jackson.databind.JsonNode;
import info.prorabka.abik.server.common.security.SecurityUtils;
import info.prorabka.abik.server.offline.dto.OfflineOperationRequest;
import info.prorabka.abik.server.offline.service.OfflineOperationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/teams/{teamId}/offline-operations")
@Tag(name = "Offline", description = "Идемпотентная синхронизация операций с Android")
@SecurityRequirement(name = "bearerAuth")
public class OfflineOperationsController {
    private final OfflineOperationService service;

    public OfflineOperationsController(OfflineOperationService service) {
        this.service = service;
    }

    @Operation(summary = "Выполнить или повторно получить результат офлайн-операции")
    @PostMapping
    public ResponseEntity<JsonNode> execute(@PathVariable Long teamId,
                                            @Valid @RequestBody OfflineOperationRequest request) {
        return ResponseEntity.ok(service.execute(teamId, SecurityUtils.getCurrentUserId(), request));
    }
}
