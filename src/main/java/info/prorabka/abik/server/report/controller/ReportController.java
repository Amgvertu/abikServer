package info.prorabka.abik.server.report.controller;

import info.prorabka.abik.server.charge.entity.ChargeStatus;
import info.prorabka.abik.server.common.security.SecurityUtils;
import info.prorabka.abik.server.report.dto.ChargeSummaryResponse;
import info.prorabka.abik.server.report.dto.MemberSummaryResponse;
import info.prorabka.abik.server.report.dto.ReportFilterRequest;
import info.prorabka.abik.server.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Отчёты и экспорт данных.
 */
@RestController
@RequestMapping("/v1/teams/{teamId}/reports")
@Tag(name = "Reports",
        description = """
             Отчёты по команде: сводки и экспорт в Excel.

             **Сводки:**
             - `summary` — общие показатели команды.
             - `members` — разбивка по каждому участнику.

             **Экспорт:**
             - `/charges/export` — все начисления в Excel с фильтрами.
             - `/members/export` — сводка по участникам в Excel.

             Все эндпоинты требуют авторизации и активного членства в команде.
             """)
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // ============================================================
    // Сводки
    // ============================================================

    @Operation(
            summary = "Сводка по команде",
            description = """
                    Возвращает:
                    - Общая сумма начислений (₽)
                    - Всего оплачено (₽)
                    - Остаток к оплате (₽)
                    - Общий баланс команды (сумма переплат − сумма долгов)
                    - Количество активных / оплаченных / частично оплаченных / отменённых начислений

                    Все суммы — **в копейках**.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Сводка по команде",
            content = @Content(schema = @Schema(implementation = ChargeSummaryResponse.class)))
    @GetMapping("/summary")
    public ResponseEntity<ChargeSummaryResponse> summary(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId) {
        Long actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(reportService.getChargeSummary(teamId, actor));
    }

    @Operation(
            summary = "Сводка по участникам",
            description = """
                    Для каждого активного участника: начислено, оплачено, остаток долга, баланс.
                    Суммы — в копейках.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Сводка по участникам",
            content = @Content(schema = @Schema(implementation = MemberSummaryResponse.class)))
    @GetMapping("/members")
    public ResponseEntity<List<MemberSummaryResponse>> members(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId) {
        Long actor = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(reportService.getMembersSummary(teamId, actor));
    }

    // ============================================================
    // Экспорт в Excel
    // ============================================================

    @Operation(
            summary = "Экспорт начислений в Excel",
            description = """
                    Формирует `.xlsx`-файл со всеми начислениями команды.

                    **Фильтры (как в списке начислений):** userId, groupId, status, dateFrom, dateTo, amountMin, amountMax.

                    Возвращает файл с заголовком `Content-Disposition: attachment`.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Excel-файл",
            content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
    @GetMapping("/charges/export")
    public ResponseEntity<byte[]> exportCharges(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) ChargeStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Long amountMin,
            @RequestParam(required = false) Long amountMax) {

        Long actor = SecurityUtils.getCurrentUserId();
        ReportFilterRequest filter = new ReportFilterRequest(
                userId, groupId, status, dateFrom, dateTo, amountMin, amountMax);

        byte[] file = reportService.exportChargesToExcel(teamId, actor, filter);

        String filename = "charges-team-" + teamId + "-" + LocalDate.now() + ".xlsx";
        return xlsxResponse(file, filename);
    }

    @Operation(
            summary = "Экспорт сводки по участникам в Excel",
            description = "Формирует `.xlsx`-файл со сводкой: ФИО, телефон, начислено, оплачено, долг, баланс. Отрицательный баланс подсвечен красным."
    )
    @ApiResponse(responseCode = "200", description = "Excel-файл",
            content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
    @GetMapping("/members/export")
    public ResponseEntity<byte[]> exportMembers(
            @Parameter(description = "ID команды", example = "1") @PathVariable Long teamId) {

        Long actor = SecurityUtils.getCurrentUserId();
        byte[] file = reportService.exportMembersToExcel(teamId, actor);

        String filename = "members-team-" + teamId + "-" + LocalDate.now() + ".xlsx";
        return xlsxResponse(file, filename);
    }

    // ============================================================
    // Helpers
    // ============================================================

    private ResponseEntity<byte[]> xlsxResponse(byte[] body, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(body.length)
                .body(body);
    }
}