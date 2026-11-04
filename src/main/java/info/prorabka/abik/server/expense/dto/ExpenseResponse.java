package info.prorabka.abik.server.expense.dto;

import info.prorabka.abik.server.expense.entity.TeamExpense;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ExpenseResponse(Long id, String category, Long amountKopecks,
                              LocalDate expenseDate, String comment, Long eventId,
                              Long createdBy, OffsetDateTime createdAt) {
    public static ExpenseResponse from(TeamExpense e) {
        return new ExpenseResponse(e.getId(), e.getCategory(), e.getAmountKopecks(),
                e.getExpenseDate(), e.getComment(), e.getEventId(), e.getCreatedBy(), e.getCreatedAt());
    }
}
