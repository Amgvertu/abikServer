package info.prorabka.abik.server.expense.service;

import info.prorabka.abik.server.common.exception.ForbiddenException;
import info.prorabka.abik.server.expense.entity.TeamExpense;
import info.prorabka.abik.server.expense.repository.TeamExpenseRepository;
import info.prorabka.abik.server.team.service.TeamMemberService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseExportServiceTest {
    @Mock TeamExpenseRepository repository;
    @Mock TeamMemberService members;
    @InjectMocks ExpenseExportService service;

    @Test
    void ownerExportFiltersPeriodAndContainsTotalFormula() throws Exception {
        when(repository.findByTeamIdOrderByExpenseDateDescIdDesc(1L)).thenReturn(List.of(
                expense(1L, "2026-11-02", 20_050L),
                expense(2L, "2026-11-01", 10_000L),
                expense(3L, "2026-10-31", 99_999L)));

        byte[] bytes = service.export(1L, 100L,
                LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-02"));

        verify(members).requireOwner(1L, 100L);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheet("Расходы");
            assertEquals("02.11.2026", sheet.getRow(4).getCell(0).getStringCellValue());
            assertEquals("01.11.2026", sheet.getRow(5).getCell(0).getStringCellValue());
            assertEquals("SUM(D5:D6)", sheet.getRow(6).getCell(3).getCellFormula());
            assertEquals(300.50, sheet.getRow(6).getCell(3).getNumericCellValue(), 0.001);
        }
    }

    @Test
    void nonOwnerCannotExport() {
        doThrow(new ForbiddenException("Только владелец")).when(members).requireOwner(1L, 100L);

        assertThrows(ForbiddenException.class, () -> service.export(1L, 100L, null, null));

        verify(repository, never()).findByTeamIdOrderByExpenseDateDescIdDesc(anyLong());
    }

    private TeamExpense expense(Long id, String date, Long amount) {
        return TeamExpense.builder()
                .id(id).teamId(1L).category("ICE").amountKopecks(amount)
                .expenseDate(LocalDate.parse(date)).comment("Тест").createdBy(100L)
                .build();
    }
}
