package info.prorabka.abik.server.charge.controller;

import info.prorabka.abik.server.charge.dto.*;
import info.prorabka.abik.server.charge.entity.ChargeStatus;
import info.prorabka.abik.server.charge.service.ChargeService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Начисления (конкретные долги пользователей).
 */
@RestController
@RequestMapping("/v1/teams/{teamId}/charges")
@Tag(name = "Charges",
        description = """
             Начисления — конкретные долги участников команды.

             **Статусы:** ACTIVE, PARTIALLY_PAID, PAID, CANCELLED, ARCHIVED.

             **Права:**
             - Чтение — активные участники команды.
             - Создание/отмена — владелец/модератор.

             Автоматическая генерация из шаблонов запускается планировщиком раз в час.
             """)
@SecurityRequirement(name = "bearerAuth")
public class ChargesController {

    private final ChargeService chargeService;

    public ChargesController(ChargeService chargeService) {
        this.chargeService = chargeService;
    }

    @Operation(summary = "Список начислений с фильтрами",
            description = """
                    Возвращает начисления команды. Фильтры опциональны — можно комбинировать.

                    **Параметры фильтра:**
                    - `userId` — конкретный участник.
                    - `groupId`, `subgroupId` — группа и подгруппа.
                    - `status` — статус начисления.
                    - `dateFrom`, `dateTo` — диапазон дат начисления (ISO, `yyyy-MM-dd`).
                    - `amountMin`, `amountMax` — диапазон суммы (в копейках).
                    """)
    @ApiResponse(responseCode = "200", description = "Список начислений",
            content = @Content(schema = @Schema(implementation = ChargeResponse.class)))
    @GetMapping
    public ResponseEntity<List<ChargeResponse>> list(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long subgroupId,
            @RequestParam(required = false) ChargeStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Long amountMin,
            @RequestParam(required = false) Long amountMax) {

        Long actor = SecurityUtils.getCurrentUserId();
        ChargeFilterRequest filter = new ChargeFilterRequest(
                userId, groupId, subgroupId, status, dateFrom, dateTo, amountMin, amountMax);
        return ResponseEntity.ok(chargeService.listCharges(teamId, actor, filter));
    }

    @Operation(summary = "Одно начисление")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Начисление",
                    content = @Content(schema = @Schema(implementation = ChargeResponse.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Начисление не найдено")
    })
    @GetMapping("/{chargeId}")
    public ResponseEntity<ChargeResponse> get(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @Parameter(description = "ID начисления", example = "1") @PathVariable Long chargeId) {
        Long actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(chargeService.getCharge(teamId, chargeId, actor));
    }

    @Operation(summary = "Создать разовое начисление вручную",
            description = """
                    Создаёт отдельные начисления для каждого указанного пользователя.

                    Сумма из `amountKopecks` назначается **каждому** выбранному
                    пользователю целиком (не делится между ними, не округляется).

                    **Права:** владелец/модератор.
                    **Ограничение:** все `userIds` должны быть активными участниками команды.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Начисления созданы",
                    content = @Content(schema = @Schema(implementation = ChargeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации или неактивный участник"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав или команда в архиве")
    })
    @PostMapping
    public ResponseEntity<List<ChargeResponse>> createManual(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @Valid @RequestBody ChargeCreateManualRequest req) {
        Long actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(chargeService.createManual(teamId, actor, req));
    }

    @Operation(summary = "Отменить начисление",
            description = """
                    Переводит начисление в статус CANCELLED. Указывается причина.

                    Если по нему уже были оплаты — при следующем платеже оплаченное
                    перераспределится на другие долги (модуль payment).

                    **Права:** владелец/модератор.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Начисление отменено",
                    content = @Content(schema = @Schema(implementation = ChargeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Начисление уже отменено или в архиве"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Начисление не найдено")
    })
    @PostMapping("/{chargeId}/cancel")
    public ResponseEntity<ChargeResponse> cancel(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @Parameter(description = "ID начисления", example = "1") @PathVariable Long chargeId,
            @Valid @RequestBody ChargeCancelRequest req) {
        Long actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(chargeService.cancel(teamId, chargeId, actor, req));
    }

    @Operation(summary = "Редактировать начисление",
            description = """
                    Меняет группу/подгруппу, сумму, комментарий и дату начисления.

                    **Ограничения:**
                    - нельзя править отменённые (CANCELLED) и архивные (ARCHIVED);
                    - новая сумма не может быть меньше уже оплаченной;
                    - участник не меняется (для переноса — отменить и создать новое).

                    **Права:** владелец/модератор.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Начисление обновлено",
                    content = @Content(schema = @Schema(implementation = ChargeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации или запрет по статусу/сумме"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Начисление не найдено")
    })
    @PutMapping("/{chargeId}")
    public ResponseEntity<ChargeResponse> update(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @Parameter(description = "ID начисления", example = "1") @PathVariable Long chargeId,
            @Valid @RequestBody ChargeUpdateRequest req) {
        Long actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(chargeService.update(teamId, chargeId, actor, req));
    }

    @Operation(summary = "Удалить начисление",
            description = """
                    Безвозвратно удаляет начисление. Разрешено, только если по нему
                    не было оплат. Если оплаты были — используйте отмену с причиной
                    (POST /{chargeId}/cancel), деньги перераспределятся на другие долги.

                    **Права:** владелец/модератор.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Начисление удалено"),
            @ApiResponse(responseCode = "400", description = "Есть оплаты или начисление в архиве"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Начисление не найдено")
    })
    @DeleteMapping("/{chargeId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @Parameter(description = "ID начисления", example = "1") @PathVariable Long chargeId) {
        Long actor = SecurityUtils.getCurrentUserId();
        chargeService.delete(teamId, chargeId, actor);
        return ResponseEntity.noContent().build();
    }
}
