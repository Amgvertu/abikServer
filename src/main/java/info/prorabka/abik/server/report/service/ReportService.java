package info.prorabka.abik.server.report.service;

import info.prorabka.abik.server.charge.entity.Charge;
import info.prorabka.abik.server.charge.entity.ChargeGroup;
import info.prorabka.abik.server.charge.entity.ChargeStatus;
import info.prorabka.abik.server.charge.repository.ChargeRepository;
import info.prorabka.abik.server.charge.service.ChargeGroupService;
import info.prorabka.abik.server.payment.entity.Balance;
import info.prorabka.abik.server.payment.repository.BalanceRepository;
import info.prorabka.abik.server.payment.repository.PaymentRepository;
import info.prorabka.abik.server.payment.service.BalanceService;
import info.prorabka.abik.server.report.dto.ChargeSummaryResponse;
import info.prorabka.abik.server.report.dto.MemberSummaryResponse;
import info.prorabka.abik.server.report.dto.ReportFilterRequest;
import info.prorabka.abik.server.team.entity.MemberStatus;
import info.prorabka.abik.server.team.entity.TeamMember;
import info.prorabka.abik.server.team.repository.TeamMemberRepository;
import info.prorabka.abik.server.team.service.TeamMemberService;
import info.prorabka.abik.server.user.entity.User;
import info.prorabka.abik.server.user.entity.UserProfile;
import info.prorabka.abik.server.user.repository.UserProfileRepository;
import info.prorabka.abik.server.user.repository.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис отчётов: сводки и экспорт в Excel.
 */
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final ChargeRepository chargeRepository;
    private final ChargeGroupService chargeGroupService;
    private final PaymentRepository paymentRepository;
    private final BalanceRepository balanceRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamMemberService teamMemberService;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final BalanceService balanceService;

    public ReportService(ChargeRepository chargeRepository,
                         ChargeGroupService chargeGroupService,
                         PaymentRepository paymentRepository,
                         BalanceRepository balanceRepository,
                         TeamMemberRepository teamMemberRepository,
                         TeamMemberService teamMemberService,
                         UserRepository userRepository,
                         UserProfileRepository userProfileRepository,
                         BalanceService balanceService) {
        this.chargeRepository = chargeRepository;
        this.chargeGroupService = chargeGroupService;
        this.paymentRepository = paymentRepository;
        this.balanceRepository = balanceRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.teamMemberService = teamMemberService;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.balanceService = balanceService;
    }

    // ============================================================
    // Сводка по команде
    // ============================================================

    @Transactional
    public ChargeSummaryResponse getChargeSummary(Long teamId, Long actorUserId) {
        teamMemberService.requireMember(teamId, actorUserId);

        List<Charge> allCharges = chargeRepository.findByTeamIdOrderByCreatedAtDesc(teamId);

        long totalCharges = 0;
        long totalPaid = 0;
        long totalRemaining = 0;
        long activeCount = 0;
        long paidCount = 0;
        long partiallyCount = 0;
        long cancelledCount = 0;

        for (Charge c : allCharges) {
            switch (c.getStatus()) {
                case ACTIVE -> {
                    totalCharges += c.getAmountKopecks();
                    totalPaid += c.getPaidKopecks();
                    totalRemaining += c.remainingKopecks();
                    activeCount++;
                }
                case PARTIALLY_PAID -> {
                    totalCharges += c.getAmountKopecks();
                    totalPaid += c.getPaidKopecks();
                    totalRemaining += c.remainingKopecks();
                    partiallyCount++;
                }
                case PAID -> {
                    totalCharges += c.getAmountKopecks();
                    totalPaid += c.getPaidKopecks();
                    paidCount++;
                }
                case CANCELLED, ARCHIVED -> cancelledCount++;
            }
        }

        // Баланс команды = сумма балансов участников
        long teamBalance = balanceRepository.findByIdTeamId(teamId).stream()
                .mapToLong(Balance::getAmountKopecks)
                .sum();

        return new ChargeSummaryResponse(
                teamId,
                totalCharges,
                totalPaid,
                totalRemaining,
                teamBalance,
                activeCount,
                paidCount,
                partiallyCount,
                cancelledCount
        );
    }

    // ============================================================
    // Сводка по участникам
    // ============================================================

    @Transactional
    public List<MemberSummaryResponse> getMembersSummary(Long teamId, Long actorUserId) {
        teamMemberService.requireMember(teamId, actorUserId);

        List<TeamMember> members = teamMemberRepository
                .findByTeamIdAndStatusOrderByJoinedAtAsc(teamId, MemberStatus.ACTIVE);

        List<MemberSummaryResponse> result = new ArrayList<>();

        for (TeamMember m : members) {
            Long userId = m.getUserId();
            User user = userRepository.findById(userId).orElse(null);
            UserProfile profile = userProfileRepository.findById(userId).orElse(null);

            String lastName = profile != null ? profile.getLastName() : null;
            String firstName = profile != null ? profile.getFirstName() : null;
            String phone = user != null ? user.getPhone() : null;

            // Агрегаты по открытым долгам (ACTIVE + PARTIALLY_PAID) — для колонок сводки
            List<Charge> debts = chargeRepository
                    .findByTeamIdAndUserIdAndStatusInOrderByDueDateAscIdAsc(
                            teamId, userId,
                            List.of(ChargeStatus.ACTIVE, ChargeStatus.PARTIALLY_PAID));

            long totalCharges = 0;
            long totalPaid = 0;
            long totalRemaining = 0;
            for (Charge c : debts) {
                totalCharges += c.getAmountKopecks();
                totalPaid += c.getPaidKopecks();
                totalRemaining += c.remainingKopecks();
            }

            // Баланс — строго по единой формуле из BalanceService (ТЗ 6.1)
            long balance = balanceService.getRawBalance(teamId, userId);

            result.add(new MemberSummaryResponse(
                    userId, lastName, firstName, phone,
                    totalCharges, totalPaid, totalRemaining, balance
            ));
        }

        return result;
    }

    // ============================================================
    // Excel — начисления
    // ============================================================

    @Transactional
    public byte[] exportChargesToExcel(Long teamId, Long actorUserId, ReportFilterRequest filter) {
        teamMemberService.requireMember(teamId, actorUserId);

        List<Charge> charges = chargeRepository.findByTeamIdOrderByCreatedAtDesc(teamId);

        // Применяем фильтры
        List<Charge> filtered = charges.stream()
                .filter(c -> filter.userId() == null || c.getUserId().equals(filter.userId()))
                .filter(c -> filter.groupId() == null || c.getGroupId().equals(filter.groupId()))
                .filter(c -> filter.status() == null || c.getStatus() == filter.status())
                .filter(c -> filter.dateFrom() == null || !c.getDueDate().isBefore(filter.dateFrom()))
                .filter(c -> filter.dateTo() == null || !c.getDueDate().isAfter(filter.dateTo()))
                .filter(c -> filter.amountMinKopecks() == null
                        || c.getAmountKopecks() >= filter.amountMinKopecks())
                .filter(c -> filter.amountMaxKopecks() == null
                        || c.getAmountKopecks() <= filter.amountMaxKopecks())
                .toList();

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Начисления");

            // Стили
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle moneyStyle = wb.createCellStyle();
            moneyStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
            CellStyle dateStyle = wb.createCellStyle();
            dateStyle.setDataFormat(wb.createDataFormat().getFormat("dd.MM.yyyy"));

            // Заголовки
            String[] headers = {
                    "ID", "Участник", "Телефон", "Группа", "Подгруппа",
                    "Сумма, ₽", "Оплачено, ₽", "Остаток, ₽",
                    "Статус", "Дата начисления", "Описание", "Период"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Данные
            int rowIdx = 1;
            for (Charge c : filtered) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(c.getId());

                UserProfile profile = userProfileRepository.findById(c.getUserId()).orElse(null);
                String name = profile != null
                        ? profile.getLastName() + " " + profile.getFirstName()
                        : "user#" + c.getUserId();
                row.createCell(1).setCellValue(name);

                User user = userRepository.findById(c.getUserId()).orElse(null);
                row.createCell(2).setCellValue(user != null ? user.getPhone() : "");

                ChargeGroup group = chargeGroupService.getGroupOrThrow(c.getGroupId());
                row.createCell(3).setCellValue(group.getName());

                row.createCell(4).setCellValue(
                        c.getSubgroupId() != null ? String.valueOf(c.getSubgroupId()) : "");

                Cell amountCell = row.createCell(5);
                amountCell.setCellValue(toRub(c.getAmountKopecks()));
                amountCell.setCellStyle(moneyStyle);

                Cell paidCell = row.createCell(6);
                paidCell.setCellValue(toRub(c.getPaidKopecks()));
                paidCell.setCellStyle(moneyStyle);

                Cell remCell = row.createCell(7);
                remCell.setCellValue(toRub(c.remainingKopecks()));
                remCell.setCellStyle(moneyStyle);

                row.createCell(8).setCellValue(c.getStatus().name());

                Cell dueCell = row.createCell(9);
                dueCell.setCellValue(c.getDueDate().format(DATE_FMT));
                dueCell.setCellStyle(dateStyle);

                row.createCell(10).setCellValue(c.getDescription() != null ? c.getDescription() : "");

                String period = "";
                if (c.getPeriodYear() != null && c.getPeriodMonth() != null) {
                    period = c.getPeriodMonth() + "/" + c.getPeriodYear();
                }
                row.createCell(11).setCellValue(period);
            }

            // Авторазмер колонок
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            return toBytes(wb);
        } catch (IOException e) {
            log.error("Ошибка генерации Excel для команды {}", teamId, e);
            throw new RuntimeException("Не удалось сгенерировать Excel-файл", e);
        }
    }

    // ============================================================
    // Excel — сводка по участникам
    // ============================================================

    @Transactional
    public byte[] exportMembersToExcel(Long teamId, Long actorUserId) {
        List<MemberSummaryResponse> members = getMembersSummary(teamId, actorUserId);

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Участники");

            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle moneyStyle = wb.createCellStyle();
            moneyStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
            CellStyle negativeStyle = wb.createCellStyle();
            negativeStyle.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
            Font redFont = wb.createFont();
            redFont.setColor(IndexedColors.RED.getIndex());
            negativeStyle.setFont(redFont);

            String[] headers = {
                    "ID", "Фамилия", "Имя", "Телефон",
                    "Начислено, ₽", "Оплачено, ₽", "Остаток долга, ₽", "Баланс, ₽"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (MemberSummaryResponse m : members) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(m.userId());
                row.createCell(1).setCellValue(nullSafe(m.lastName()));
                row.createCell(2).setCellValue(nullSafe(m.firstName()));
                row.createCell(3).setCellValue(nullSafe(m.phone()));

                moneyCell(row, 4, m.totalChargesKopecks(), moneyStyle);
                moneyCell(row, 5, m.totalPaidKopecks(), moneyStyle);
                moneyCell(row, 6, m.totalRemainingKopecks(), moneyStyle);

                Cell balanceCell = row.createCell(7);
                balanceCell.setCellValue(toRub(m.balanceKopecks()));
                balanceCell.setCellStyle(m.balanceKopecks() < 0 ? negativeStyle : moneyStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            return toBytes(wb);
        } catch (IOException e) {
            log.error("Ошибка генерации Excel-сводки по участникам команды {}", teamId, e);
            throw new RuntimeException("Не удалось сгенерировать Excel-файл", e);
        }
    }

    // ============================================================
    // Helpers
    // ============================================================

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private void moneyCell(Row row, int col, Long kopecks, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(toRub(kopecks));
        cell.setCellStyle(style);
    }

    /** Копейки → рубли (double для Excel). */
    private double toRub(Long kopecks) {
        return kopecks == null ? 0.0 : kopecks / 100.0;
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private byte[] toBytes(Workbook wb) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return out.toByteArray();
    }
}