package info.prorabka.abik.server.expense.controller;

import info.prorabka.abik.server.common.security.SecurityUtils;
import info.prorabka.abik.server.expense.dto.*;
import info.prorabka.abik.server.expense.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/v1/teams/{teamId}/expenses")
public class ExpensesController {
    private final ExpenseService service;
    public ExpensesController(ExpenseService service) { this.service = service; }
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
