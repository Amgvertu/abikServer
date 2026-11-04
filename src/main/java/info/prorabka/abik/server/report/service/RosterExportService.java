package info.prorabka.abik.server.report.service;

import info.prorabka.abik.server.charge.entity.ChargeList;
import info.prorabka.abik.server.charge.repository.ChargeListRepository;
import info.prorabka.abik.server.common.exception.BadRequestException;
import info.prorabka.abik.server.common.exception.NotFoundException;
import info.prorabka.abik.server.team.entity.MemberStatus;
import info.prorabka.abik.server.team.entity.TeamMember;
import info.prorabka.abik.server.team.repository.TeamMemberRepository;
import info.prorabka.abik.server.team.service.TeamMemberService;
import info.prorabka.abik.server.user.entity.UserProfile;
import info.prorabka.abik.server.user.repository.UserProfileRepository;
import info.prorabka.abik.server.user.repository.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

@Service
public class RosterExportService {
    private final TeamMemberService memberService;
    private final TeamMemberRepository members;
    private final ChargeListRepository lists;
    private final UserRepository users;
    private final UserProfileRepository profiles;
    public RosterExportService(TeamMemberService memberService, TeamMemberRepository members,
                               ChargeListRepository lists, UserRepository users, UserProfileRepository profiles) {
        this.memberService = memberService; this.members = members; this.lists = lists;
        this.users = users; this.profiles = profiles;
    }
    public byte[] export(Long teamId, Long listId, Long actor) {
        memberService.requireMember(teamId, actor);
        Set<Long> allowed = null;
        String title = "Вся команда";
        if (listId != null) {
            ChargeList list = lists.findById(listId).orElseThrow(() -> new NotFoundException("Список не найден"));
            if (!list.getTeamId().equals(teamId)) throw new NotFoundException("Список не найден");
            allowed = list.getMemberIds(); title = list.getName();
        }
        List<TeamMember> rows = new ArrayList<>(members.findByTeamIdAndStatusOrderByJoinedAtAsc(teamId, MemberStatus.ACTIVE));
        Set<Long> allowedMembers = allowed;
        if (allowedMembers != null) rows.removeIf(m -> !allowedMembers.contains(m.getUserId()));
        rows.sort(Comparator.comparing(this::positionRank).thenComparing(this::name));
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Состав");
            String[] headers = {"Номер", "Фамилия", "Имя", "Телефон", "Роль", "Тренер", "Амплуа"};
            Row head = sheet.createRow(0);
            CellStyle style = workbook.createCellStyle(); Font font = workbook.createFont();
            font.setBold(true); style.setFont(font);
            for (int i = 0; i < headers.length; i++) { head.createCell(i).setCellValue(headers[i]); head.getCell(i).setCellStyle(style); }
            int rowIndex = 1;
            for (TeamMember member : rows) {
                UserProfile profile = profiles.findById(member.getUserId()).orElse(null);
                Row row = sheet.createRow(rowIndex++);
                if (member.getJerseyNumber() != null) row.createCell(0).setCellValue(member.getJerseyNumber());
                row.createCell(1).setCellValue(profile == null ? "" : profile.getLastName());
                row.createCell(2).setCellValue(profile == null ? "" : profile.getFirstName());
                row.createCell(3).setCellValue(users.findById(member.getUserId()).map(u -> u.getPhone()).orElse(""));
                row.createCell(4).setCellValue(member.getRole().name());
                row.createCell(5).setCellValue(Boolean.TRUE.equals(member.getIsTrainer()) ? "Да" : "Нет");
                row.createCell(6).setCellValue(profile == null ? "" : profile.getPositions().stream().map(Enum::name).sorted().reduce((a,b) -> a + ", " + b).orElse(""));
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            workbook.write(out); return out.toByteArray();
        } catch (IOException e) { throw new BadRequestException("Не удалось сформировать Excel"); }
    }
    private int positionRank(TeamMember m) {
        if (Boolean.TRUE.equals(m.getIsTrainer())) return 4;
        UserProfile p = profiles.findById(m.getUserId()).orElse(null);
        if (p == null) return 3;
        if (p.getPositions().stream().anyMatch(p2 -> p2.name().equals("GOALKEEPER"))) return 0;
        if (p.getPositions().stream().anyMatch(p2 -> p2.name().equals("DEFENDER"))) return 1;
        if (p.getPositions().stream().anyMatch(p2 -> p2.name().equals("FORWARD"))) return 2;
        return 3;
    }
    private String name(TeamMember m) {
        return profiles.findById(m.getUserId()).map(p -> p.getLastName() + " " + p.getFirstName()).orElse("");
    }
}
