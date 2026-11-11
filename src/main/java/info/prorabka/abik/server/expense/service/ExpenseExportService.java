package info.prorabka.abik.server.expense.service;

import info.prorabka.abik.server.expense.entity.TeamExpense;
import info.prorabka.abik.server.expense.repository.TeamExpenseRepository;
import info.prorabka.abik.server.team.service.TeamMemberService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** Excel-отчёт по расходам команды. Доступен только владельцу. */
@Service
public class ExpenseExportService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Map<String, String> CATEGORIES = Map.of(
            "ICE", "Лёд",
            "COACH", "Тренер",
            "GAME", "Игра",
            "GOALKEEPER", "Вратарь",
            "OTHER", "Прочее"
    );

    private final TeamExpenseRepository repository;
    private final TeamMemberService members;

    public ExpenseExportService(TeamExpenseRepository repository, TeamMemberService members) {
        this.repository = repository;
        this.members = members;
    }

    @Transactional(readOnly = true)
    public byte[] export(Long teamId, Long actor, LocalDate dateFrom, LocalDate dateTo) {
        members.requireOwner(teamId, actor);
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new info.prorabka.abik.server.common.exception.BadRequestException(
                    "Дата начала периода не может быть позже даты окончания");
        }
        List<TeamExpense> expenses = repository.findByTeamIdOrderByExpenseDateDescIdDesc(teamId).stream()
                .filter(e -> dateFrom == null || !e.getExpenseDate().isBefore(dateFrom))
                .filter(e -> dateTo == null || !e.getExpenseDate().isAfter(dateTo))
                .toList();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Расходы");
            sheet.setDisplayGridlines(false);

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00\" ₽\""));
            CellStyle totalStyle = workbook.createCellStyle();
            totalStyle.cloneStyleFrom(moneyStyle);
            totalStyle.setFont(headerFont);

            Row title = sheet.createRow(0);
            title.createCell(0).setCellValue("Отчёт по расходам команды");
            title.getCell(0).setCellStyle(titleStyle);
            String period = (dateFrom == null ? "…" : dateFrom.format(DATE)) + " — "
                    + (dateTo == null ? "…" : dateTo.format(DATE));
            sheet.createRow(1).createCell(0).setCellValue("Период: " + period);

            String[] headers = {"Дата", "Категория", "Комментарий", "Сумма"};
            Row header = sheet.createRow(3);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 4;
            for (TeamExpense expense : expenses) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(expense.getExpenseDate().format(DATE));
                row.createCell(1).setCellValue(CATEGORIES.getOrDefault(expense.getCategory(), expense.getCategory()));
                row.createCell(2).setCellValue(expense.getComment() == null ? "" : expense.getComment());
                Cell amount = row.createCell(3);
                amount.setCellValue(expense.getAmountKopecks() / 100.0);
                amount.setCellStyle(moneyStyle);
            }
            if (expenses.isEmpty()) {
                sheet.createRow(rowIndex++).createCell(0).setCellValue("Нет расходов за выбранный период");
            }

            Row total = sheet.createRow(rowIndex);
            Cell totalLabel = total.createCell(2);
            totalLabel.setCellValue("Итого");
            totalLabel.setCellStyle(headerStyle);
            Cell totalAmount = total.createCell(3);
            totalAmount.setCellFormula("SUM(D5:D" + rowIndex + ")");
            totalAmount.setCellStyle(totalStyle);

            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(3, 3, 0, headers.length - 1));
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось сформировать Excel-отчёт по расходам", e);
        }
    }
}
