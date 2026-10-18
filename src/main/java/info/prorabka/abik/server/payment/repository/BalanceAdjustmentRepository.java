package info.prorabka.abik.server.payment.repository;

import info.prorabka.abik.server.payment.entity.BalanceAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BalanceAdjustmentRepository extends JpaRepository<BalanceAdjustment, Long> {

    /** История корректировок участника, свежие сверху. */
    List<BalanceAdjustment> findByTeamIdAndUserIdOrderByCreatedAtDesc(Long teamId, Long userId);

    /** Сумма корректировок (для расчёта баланса). */
    @Query("""
            SELECT COALESCE(SUM(b.amountKopecks), 0)
            FROM BalanceAdjustment b
            WHERE b.teamId = :teamId AND b.userId = :userId
            """)
    long sumByTeamAndUser(@Param("teamId") Long teamId, @Param("userId") Long userId);
}