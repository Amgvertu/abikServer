package info.prorabka.abik.server.event.controller;

import info.prorabka.abik.server.common.security.SecurityUtils;
import info.prorabka.abik.server.event.dto.*;
import info.prorabka.abik.server.event.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/teams/{teamId}/events")
@Tag(name = "Events", description = "Тренировки, игры, матчи")
@SecurityRequirement(name = "bearerAuth")
public class EventsController {

    private final EventService service;

    public EventsController(EventService service) {
        this.service = service;
    }

    @Operation(summary = "События команды за период")
    @GetMapping
    public ResponseEntity<List<EventListItemResponse>> list(
            @PathVariable Long teamId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Long actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(service.list(teamId, actor, from, to));
    }

    @Operation(summary = "Детали события")
    @GetMapping("/{eventId}")
    public ResponseEntity<EventDetailResponse> get(
            @PathVariable Long teamId, @PathVariable Long eventId) {
        Long actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(service.getDetail(teamId, eventId, actor));
    }

    @Operation(summary = "Создать событие (в т.ч. серию)")
    @PostMapping
    public ResponseEntity<EventDetailResponse> create(
            @PathVariable Long teamId, @Valid @RequestBody EventCreateRequest req) {
        Long actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(service.create(teamId, actor, req));
    }

    @Operation(summary = "Обновить событие")
    @PutMapping("/{eventId}")
    public ResponseEntity<EventDetailResponse> update(
            @PathVariable Long teamId, @PathVariable Long eventId,
            @RequestParam(required = false, defaultValue = "single") String scope,
            @Valid @RequestBody EventUpdateRequest req) {
        Long actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(service.update(teamId, eventId, actor, req, scope));
    }

    @Operation(summary = "Удалить событие")
    @DeleteMapping("/{eventId}")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable Long teamId, @PathVariable Long eventId,
            @RequestParam(required = false, defaultValue = "single") String scope) {
        Long actor = SecurityUtils.getCurrentUserId();
        service.delete(teamId, eventId, actor, scope);
        return ResponseEntity.ok(Map.of("message", "Удалено"));
    }

    @Operation(summary = "Проголосовать")
    @PutMapping("/{eventId}/vote")
    public ResponseEntity<Map<String, String>> vote(
            @PathVariable Long teamId, @PathVariable Long eventId,
            @Valid @RequestBody VoteRequest req) {
        Long actor = SecurityUtils.getCurrentUserId();
        service.vote(teamId, eventId, actor, req.answer());
        return ResponseEntity.ok(Map.of("message", "Голос учтён"));
    }

    @Operation(summary = "Назначить цвета свитеров")
    @PutMapping("/{eventId}/sweater-colors")
    public ResponseEntity<Map<String, String>> setSweaterColors(
            @PathVariable Long teamId, @PathVariable Long eventId,
            @RequestBody List<SweaterColorItemDto> items) {
        Long actor = SecurityUtils.getCurrentUserId();
        service.setSweaterColors(teamId, eventId, actor, items);
        return ResponseEntity.ok(Map.of("message", "Цвета обновлены"));
    }

    @Operation(summary = "Сохранить звенья (lineup)")
    @PutMapping("/{eventId}/lineup")
    public ResponseEntity<Map<String, String>> setLineup(
            @PathVariable Long teamId, @PathVariable Long eventId,
            @RequestBody LineupSaveRequest req) {
        Long actor = SecurityUtils.getCurrentUserId();
        service.setLineup(teamId, eventId, actor, req);
        return ResponseEntity.ok(Map.of("message", "Состав сохранён"));
    }
}