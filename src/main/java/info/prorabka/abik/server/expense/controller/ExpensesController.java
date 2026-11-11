package info.prorabka.abik.server.expense.controller;

import info.prorabka.abik.server.common.security.SecurityUtils;
import info.prorabka.abik.server.expense.dto.*;
import info.prorabka.abik.server.expense.service.ExpenseService;
import info.prorabka.abik.server.expense.service.ExpenseExportService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/teams/{teamId}/expenses")
public class ExpensesController {
    private final ExpenseService service;
    private final ExpenseExportService exportService;
    public ExpensesController(ExpenseService service, ExpenseExportService exportService) {
        this.service = service;
        this.exportService = exportService;
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @PathVariable Long teamId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        byte[] body = exportService.export(teamId, SecurityUtils.getCurrentUserId(), dateFrom, dateTo);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"expenses-team-" + teamId + ".xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(body.length)
                .body(body);
    }
    @GetMapping public List<ExpenseResponse> list(@PathVariable Long teamId) {
        return service.list(teamId, SecurityUtils.getCurrentUserId());
    }
    @PostMapping public ExpenseResponse create(@PathVariable Long teamId, @Valid @RequestBody ExpenseRequest req) {
        return service.create(teamId, SecurityUtils.getCurrentUserId(), req);
    }
    @PutMapping("/{id}") public ExpenseResponse update(@PathVariable Long teamId, @PathVariable Long id,
                                                        @Valid @RequestBody ExpenseRequest req) {
        return service.update(teamId, id, SecurityUtils.getCurrentUserId(), req);
    }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long teamId, @PathVariable Long id) {
        service.delete(teamId, id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }
}
