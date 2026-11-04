package info.prorabka.abik.server.expense.repository;

import info.prorabka.abik.server.expense.entity.TeamExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TeamExpenseRepository extends JpaRepository<TeamExpense, Long> {
    List<TeamExpense> findByTeamIdOrderByExpenseDateDescIdDesc(Long teamId);
}
