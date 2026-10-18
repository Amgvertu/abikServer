package info.prorabka.abik.server.payment.controller;

import info.prorabka.abik.server.common.security.SecurityUtils;
import info.prorabka.abik.server.payment.dto.BalanceAdjustmentRequest;
import info.prorabka.abik.server.payment.dto.BalanceAdjustmentResponse;
import info.prorabka.abik.server.payment.dto.BalanceResponse;
import info.prorabka.abik.server.payment.service.BalanceService;
import info.prorabka.abik.server.team.service.TeamMemberService;
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
 * Балансы участников команды.
 */
@RestController
@RequestMapping("/v1/teams/{teamId}/balances")
@Tag(name = "Balances",
        description = """
             Баланс участника = **сумма активных платежей − сумма активных долгов**.

             **Интерпретация:**
             - Положительный — **переплата** (человек «в запасе»).
             - Отрицательный — **долг**.
             - Ноль — всё закрыто.

             Значения пересчитываются на лету при каждом запросе.

             **Права:** активные участники команды.
             """)
@SecurityRequirement(name = "bearerAuth")
public class BalancesController {

    private final BalanceService balanceService;
    private final TeamMemberService teamMemberService; // ⬅️ Добавили

    public BalancesController(BalanceService balanceService,
                              TeamMemberService teamMemberService) {
        this.balanceService = balanceService;
        this.teamMemberService = teamMemberService;
    }

    @Operation(
            summary = "Балансы всех участников",
            description = "Список балансов активных участников команды."
    )
    @ApiResponse(responseCode = "200", description = "Список балансов",
            content = @Content(schema = @Schema(implementation = BalanceResponse.class)))
    @GetMapping
    public ResponseEntity<List<BalanceResponse>> list(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId) {
        Long actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(balanceService.getAllBalances(teamId, actor));
    }

    @Operation(
            summary = "Баланс конкретного участника",
            description = "Возвращает баланс одного пользователя в команде."
    )
    @ApiResponse(responseCode = "200", description = "Баланс",
            content = @Content(schema = @Schema(implementation = BalanceResponse.class)))
    @GetMapping("/{userId}")
    public ResponseEntity<BalanceResponse> get(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @Parameter(description = "ID участника", example = "2") @PathVariable Long userId) {
        Long actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(balanceService.getBalance(teamId, userId, actor));
    }

    @Operation(summary = "Выставить / скорректировать баланс",
            description = """
                    Ручная корректировка баланса участника.
                    
                    **Сумма в копейках:**
                    - Положительная — переплата.
                    - Отрицательная — долг.
                    
                    **Доступ:** Владелец, модератор или Суперадмин.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Корректировка применена",
                    content = @Content(schema = @Schema(implementation = BalanceAdjustmentResponse.class))),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
            @ApiResponse(responseCode = "404", description = "Участник не найден")
    })
    @PostMapping("/{userId}/adjust")
    public ResponseEntity<BalanceAdjustmentResponse> adjust(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @Parameter(description = "ID участника", example = "2") @PathVariable Long userId,
            @Valid @RequestBody BalanceAdjustmentRequest req) {
        Long actor = SecurityUtils.getCurrentUserId();
        // Проверка прав
        teamMemberService.requireManagerOrSuperAdmin(teamId, actor);
        return ResponseEntity.ok(balanceService.adjust(teamId, userId, actor, req));
    }

    @Operation(summary = "История корректировок баланса",
            description = "Список всех ручных корректировок участника. **Доступ:** Владелец, модератор или Суперадмин.")
    @ApiResponse(responseCode = "200", description = "История корректировок",
            content = @Content(schema = @Schema(implementation = BalanceAdjustmentResponse.class)))
    @GetMapping("/{userId}/adjustments")
    public ResponseEntity<List<BalanceAdjustmentResponse>> getAdjustments(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @Parameter(description = "ID участника", example = "2") @PathVariable Long userId) {
        Long actor = SecurityUtils.getCurrentUserId();
        teamMemberService.requireManagerOrSuperAdmin(teamId, actor);
        return ResponseEntity.ok(balanceService.getAdjustmentHistory(teamId, userId));
    }
}