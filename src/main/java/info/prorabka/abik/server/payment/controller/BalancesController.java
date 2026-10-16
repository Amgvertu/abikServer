package info.prorabka.abik.server.payment.controller;

import info.prorabka.abik.server.common.security.SecurityUtils;
import info.prorabka.abik.server.payment.dto.BalanceResponse;
import info.prorabka.abik.server.payment.service.BalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    public BalancesController(BalanceService balanceService) {
        this.balanceService = balanceService;
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
}