package info.prorabka.abik.server.charge.controller;

import info.prorabka.abik.server.charge.dto.ChargeListRequest;
import info.prorabka.abik.server.charge.dto.ChargeListResponse;
import info.prorabka.abik.server.charge.service.ChargeListService;
import info.prorabka.abik.server.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/teams/{teamId}/charge-lists")
@Tag(name = "Charge Lists", description = "Именованные списки получателей начислений")
@SecurityRequirement(name = "bearerAuth")
public class ChargeListsController {

    private final ChargeListService service;

    public ChargeListsController(ChargeListService service) {
        this.service = service;
    }

    @Operation(summary = "Списки получателей команды")
    @GetMapping
    public ResponseEntity<List<ChargeListResponse>> list(@PathVariable Long teamId) {
        Long actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(service.list(teamId, actor));
    }

    @Operation(summary = "Создать список")
    @PostMapping
    public ResponseEntity<ChargeListResponse> create(
            @PathVariable Long teamId,
            @Valid @RequestBody ChargeListRequest req) {
        Long actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(service.create(teamId, actor, req));
    }

    @Operation(summary = "Обновить список")
    @PutMapping("/{listId}")
    public ResponseEntity<ChargeListResponse> update(
            @PathVariable Long teamId,
            @PathVariable Long listId,
            @Valid @RequestBody ChargeListRequest req) {
        Long actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(service.update(teamId, listId, actor, req));
    }

    @Operation(summary = "Удалить список")
    @DeleteMapping("/{listId}")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable Long teamId,
            @PathVariable Long listId) {
        Long actor = SecurityUtils.getCurrentUserId();
        service.delete(teamId, listId, actor);
        return ResponseEntity.ok(Map.of("message", "Список удалён"));
    }
}
