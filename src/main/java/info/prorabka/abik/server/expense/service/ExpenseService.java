package info.prorabka.abik.server.expense.service;

import info.prorabka.abik.server.common.exception.NotFoundException;
import info.prorabka.abik.server.expense.dto.*;
import info.prorabka.abik.server.expense.entity.TeamExpense;
import info.prorabka.abik.server.expense.repository.TeamExpenseRepository;
import info.prorabka.abik.server.team.service.TeamMemberService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ExpenseService {
    private final TeamExpenseRepository repository;
    private final TeamMemberService members;
    public ExpenseService(TeamExpenseRepository repository, TeamMemberService members) {
        this.repository = repository; this.members = members;
    }
    public List<ExpenseResponse> list(Long teamId, Long actor) {
        members.requireManager(teamId, actor);
        return repository.findByTeamIdOrderByExpenseDateDescIdDesc(teamId)
                .stream().map(ExpenseResponse::from).toList();
    }
    @Transactional public ExpenseResponse create(Long teamId, Long actor, ExpenseRequest req) {
        members.requireManager(teamId, actor);
        return ExpenseResponse.from(repository.save(TeamExpense.builder().teamId(teamId)
                .category(req.category().trim()).amountKopecks(req.amountKopecks())
                .expenseDate(req.expenseDate()).comment(trim(req.comment())).eventId(req.eventId())
                .createdBy(actor).build()));
    }
    @Transactional public ExpenseResponse update(Long teamId, Long id, Long actor, ExpenseRequest req) {
        members.requireMembershipManager(teamId, actor);
        TeamExpense e = get(teamId, id);
        e.setCategory(req.category().trim()); e.setAmountKopecks(req.amountKopecks());
        e.setExpenseDate(req.expenseDate()); e.setComment(trim(req.comment())); e.setEventId(req.eventId());
        return ExpenseResponse.from(repository.save(e));
    }
    @Transactional public void delete(Long teamId, Long id, Long actor) {
        members.requireMembershipManager(teamId, actor);
        repository.delete(get(teamId, id));
    }
    private TeamExpense get(Long teamId, Long id) {
        TeamExpense e = repository.findById(id).orElseThrow(() -> new NotFoundException("Расход не найден"));
        if (!e.getTeamId().equals(teamId)) throw new NotFoundException("Расход не найден");
        return e;
    }
    private static String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
