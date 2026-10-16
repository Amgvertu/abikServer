package info.prorabka.abik.server.payment.controller;

import info.prorabka.abik.server.common.security.SecurityUtils;
import info.prorabka.abik.server.payment.dto.*;
import info.prorabka.abik.server.payment.service.PaymentService;
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
import java.util.Map;

/**
 * Платежи участников команды.
 */
@RestController
@RequestMapping("/v1/teams/{teamId}/payments")
@Tag(name = "Payments",
        description = """
             Платежи участников команды.

             **Логика автораспределения:** при внесении платежа сервер автоматически распределяет
             сумму по активным долгам получателя — сначала по самым старым (по дате начисления, затем по id).
             Если платёж больше общей задолженности — остаток остаётся в переплате (балансе).

             **Права:**
             - Внесение / редактирование / удаление — только владелец или модератор.
             - Просмотр — активные участники команды.

             **Способы оплаты** (наличные, карта, перевод) не указываются — по ТЗ.
             """)
@SecurityRequirement(name = "bearerAuth")
public class PaymentsController {

    private final PaymentService paymentService;

    public PaymentsController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // ============================================================
    // Внесение
    // ============================================================

    @Operation(
            summary = "Внести платёж",
            description = """
                    Создаёт платёж за указанного участника и **автоматически распределяет**
                    сумму по его активным долгам (сначала старые).

                    Что вернётся: созданный платёж. Распределение по начислениям можно посмотреть
                    в `GET /v1/teams/{teamId}/charges` — там поля `paidKopecks` и `status` обновятся.

                    **Права:** владелец/модератор.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Платёж внесён",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации или получатель не участник"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав или команда в архиве")
    })
    @PostMapping
    public ResponseEntity<PaymentResponse> create(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @Valid @RequestBody PaymentCreateRequest req) {
        Long actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(paymentService.create(teamId, actor, req));
    }

    // ============================================================
    // Чтение
    // ============================================================

    @Operation(
            summary = "Список платежей",
            description = """
                    Платежи команды с опциональными фильтрами.

                    **Параметры фильтра:**
                    - `userId` — только платежи конкретного участника.
                    - `dateFrom`, `dateTo` — диапазон дат создания (ISO `yyyy-MM-dd`).
                    - `amountMin`, `amountMax` — диапазон суммы в копейках.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Список платежей",
            content = @Content(schema = @Schema(implementation = PaymentResponse.class)))
    @GetMapping
    public ResponseEntity<List<PaymentResponse>> list(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Long amountMin,
            @RequestParam(required = false) Long amountMax) {

        Long actor = SecurityUtils.getCurrentUserId();
        PaymentFilterRequest filter = new PaymentFilterRequest(userId, dateFrom, dateTo, amountMin, amountMax);
        return ResponseEntity.ok(paymentService.list(teamId, actor, filter));
    }

    @Operation(summary = "Один платёж")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Платёж",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Платёж не найден")
    })
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> get(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @Parameter(description = "ID платежа", example = "1") @PathVariable Long paymentId) {
        Long actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(paymentService.get(teamId, paymentId, actor));
    }

    // ============================================================
    // Редактирование
    // ============================================================

    @Operation(
            summary = "Редактировать платёж",
            description = """
                    Меняет сумму и/или описание. **Перераспределение пересчитывается заново:**
                    сервер откатывает старые аллокации и распределяет новую сумму по долгам получателя.

                    `userId` (за кого платёж) изменить нельзя.

                    **Права:** владелец/модератор.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Платёж обновлён",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Платёж не найден")
    })
    @PutMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> update(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @Parameter(description = "ID платежа", example = "1") @PathVariable Long paymentId,
            @Valid @RequestBody PaymentUpdateRequest req) {
        Long actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(paymentService.update(teamId, paymentId, actor, req));
    }

    // ============================================================
    // Удаление
    // ============================================================

    @Operation(
            summary = "Удалить платёж",
            description = """
                    Soft-delete: платёж помечается удалённым, история сохраняется.
                    **Распределение откатывается** — долги возвращаются в исходное состояние.

                    **Права:** владелец/модератор.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Платёж удалён"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Платёж не найден")
    })
    @DeleteMapping("/{paymentId}")
    public ResponseEntity<Map<String, String>> delete(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @Parameter(description = "ID платежа", example = "1") @PathVariable Long paymentId) {
        Long actor = SecurityUtils.getCurrentUserId();
        paymentService.delete(teamId, paymentId, actor);
        return ResponseEntity.ok(Map.of("message", "Платёж удалён"));
    }
}