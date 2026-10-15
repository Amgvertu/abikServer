package info.prorabka.abik.server.charge.controller;

import info.prorabka.abik.server.charge.dto.ChargeTemplateRequest;
import info.prorabka.abik.server.charge.dto.ChargeTemplateResponse;
import info.prorabka.abik.server.charge.service.ChargeTemplateService;
import info.prorabka.abik.server.common.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Шаблоны начислений.
 */
@RestController
@RequestMapping("/v1/teams/{teamId}/charge-templates")
@Tag(name = "Charge Templates",
        description = """
             Шаблоны начислений — правила, по которым генерируются периодические начисления.

             **Типы:**
             - **SYSTEMATIC** — ежемесячное начисление с указанным днём месяца (1–28).
             - **ONE_TIME** — разовое начисление (без дня месяца).

             Шаблоны **не удаляются**, а деактивируются, чтобы сохранить историю.

             **Права:** чтение — активные участники; управление — владелец/модератор.
             """)
@SecurityRequirement(name = "bearerAuth")
public class ChargeTemplatesController {

    private final ChargeTemplateService templateService;

    public ChargeTemplatesController(ChargeTemplateService templateService) {
        this.templateService = templateService;
    }

    @Operation(summary = "Список шаблонов",
            description = "Все шаблоны команды. `onlyActive=true` вернёт только активные.")
    @ApiResponse(responseCode = "200", description = "Список шаблонов",
            content = @Content(schema = @Schema(implementation = ChargeTemplateResponse.class)))
    @GetMapping
    public ResponseEntity<List<ChargeTemplateResponse>> list(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @Parameter(description = "Только активные", example = "false")
            @RequestParam(name = "onlyActive", required = false, defaultValue = "false")
            Boolean onlyActive) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(templateService.listTemplates(teamId, userId, onlyActive));
    }

    @Operation(summary = "Один шаблон")
    @ApiResponse(responseCode = "200", description = "Шаблон",
            content = @Content(schema = @Schema(implementation = ChargeTemplateResponse.class)))
    @GetMapping("/{templateId}")
    public ResponseEntity<ChargeTemplateResponse> get(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @Parameter(description = "ID шаблона", example = "1") @PathVariable Long templateId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(templateService.getTemplate(teamId, templateId, userId));
    }

    @Operation(summary = "Создать шаблон",
            description = """
                    **Обязательные поля:** groupId, name, amountKopecks, type, periodicity,
                    distribution, memberIds (хотя бы один).

                    **Для SYSTEMATIC:** обязателен dayOfMonth (1–28).
                    **Для ONE_TIME:** dayOfMonth не указывается.

                    **Доступ:** владелец/модератор.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Шаблон создан",
                    content = @Content(schema = @Schema(implementation = ChargeTemplateResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    @PostMapping
    public ResponseEntity<ChargeTemplateResponse> create(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @Valid @RequestBody ChargeTemplateRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(templateService.create(teamId, userId, req));
    }

    @Operation(summary = "Обновить шаблон", description = "**Доступ:** владелец/модератор.")
    @ApiResponse(responseCode = "200", description = "Шаблон обновлён",
            content = @Content(schema = @Schema(implementation = ChargeTemplateResponse.class)))
    @PutMapping("/{templateId}")
    public ResponseEntity<ChargeTemplateResponse> update(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @Parameter(description = "ID шаблона", example = "1") @PathVariable Long templateId,
            @Valid @RequestBody ChargeTemplateRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(templateService.update(teamId, templateId, userId, req));
    }

    @Operation(summary = "Деактивировать шаблон",
            description = "Начисления по нему больше не генерируются. Старые начисления сохраняются. **Доступ:** владелец/модератор.")
    @ApiResponse(responseCode = "200", description = "Шаблон деактивирован",
            content = @Content(schema = @Schema(implementation = ChargeTemplateResponse.class)))
    @PostMapping("/{templateId}/deactivate")
    public ResponseEntity<ChargeTemplateResponse> deactivate(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @Parameter(description = "ID шаблона", example = "1") @PathVariable Long templateId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(templateService.deactivate(teamId, templateId, userId));
    }

    @Operation(summary = "Активировать шаблон", description = "**Доступ:** владелец/модератор.")
    @ApiResponse(responseCode = "200", description = "Шаблон активирован",
            content = @Content(schema = @Schema(implementation = ChargeTemplateResponse.class)))
    @PostMapping("/{templateId}/activate")
    public ResponseEntity<ChargeTemplateResponse> activate(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @Parameter(description = "ID шаблона", example = "1") @PathVariable Long templateId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(templateService.activate(teamId, templateId, userId));
    }
}