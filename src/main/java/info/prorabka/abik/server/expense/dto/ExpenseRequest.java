package info.prorabka.abik.server.expense.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record ExpenseRequest(
        @NotBlank @Size(max = 80) String category,
        @NotNull @Positive Long amountKopecks,
        @NotNull LocalDate expenseDate,
        @Size(max = 2000) String comment,
        Long eventId) {}
