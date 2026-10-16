package info.prorabka.abik.server.payment.repository;

import info.prorabka.abik.server.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** Активные платежи команды, свежие сверху. */
    List<Payment> findByTeamIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long teamId);

    /** Активные платежи конкретного участника. */
    List<Payment> findByTeamIdAndUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long teamId, Long userId);

    /** Один активный платёж. */
    Optional<Payment> findByIdAndDeletedAtIsNull(Long id);

    /** Сумма активных платежей пользователя (в копейках). */
    @org.springframework.data.jpa.repository.Query("""
            SELECT COALESCE(SUM(p.amountKopecks), 0)
            FROM Payment p
            WHERE p.teamId = :teamId
              AND p.userId = :userId
              AND p.deletedAt IS NULL
            """)
    long sumActiveByTeamAndUser(@org.springframework.data.repository.query.Param("teamId") Long teamId,
                                @org.springframework.data.repository.query.Param("userId") Long userId);
}
