package info.prorabka.abik.server.roster.controller;

import info.prorabka.abik.server.common.security.SecurityUtils;
import info.prorabka.abik.server.roster.dto.*;
import info.prorabka.abik.server.roster.service.RosterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/teams/{teamId}/seasons")
@Tag(name = "Roster", description = "Состав регулярки по сезонам")
@SecurityRequirement(name = "bearerAuth")
public class RosterController {

    private final RosterService service;

    public RosterController(RosterService service) {
        this.service = service;
    }

    @Operation(summary = "Список сезонов (с признаком текущего)")
    @GetMapping
    public ResponseEntity<List<SeasonResponse>> seasons(@PathVariable Long teamId) {
        Long actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(service.listSeasons(teamId, actor));
    }

    @Operation(summary = "Состав сезона")
    @GetMapping("/roster")
    public ResponseEntity<List<RosterItemResponse>> roster(
            @PathVariable Long teamId, @RequestParam String season) {
        Long actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(service.listRoster(teamId, season, actor));
    }

    @Operation(summary = "Полная замена состава")
    @PutMapping("/roster")
    public ResponseEntity<Map<String, String>> replace(
            @PathVariable Long teamId, @RequestParam String season,
            @Valid @RequestBody RosterReplaceRequest req) {
        Long actor = SecurityUtils.getCurrentUserId();
        service.replaceRoster(teamId, season, actor, req);
        return ResponseEntity.ok(Map.of("message", "Состав обновлён"));
    }

    @Operation(summary = "Добавить игрока (опционально — со стартовым взносом)")
    @PostMapping("/roster/members")
    public ResponseEntity<RosterItemResponse> add(
            @PathVariable Long teamId, @RequestParam String season,
            @Valid @RequestBody RosterAddRequest req) {
        Long actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(service.addMember(teamId, season, actor, req));
    }

    @Operation(summary = "Убрать игрока из состава")
    @DeleteMapping("/roster/members/{userId}")
    public ResponseEntity<Map<String, String>> remove(
            @PathVariable Long teamId,
            @PathVariable Long userId,
            @RequestParam String season) {
        Long actor = SecurityUtils.getCurrentUserId();
        service.removeMember(teamId, season, userId, actor);
        return ResponseEntity.ok(Map.of("message", "Игрок убран из состава"));
    }
}