package info.prorabka.abik.server.payment.repository;

import info.prorabka.abik.server.payment.entity.Balance;
import info.prorabka.abik.server.payment.entity.BalanceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BalanceRepository extends JpaRepository<Balance, BalanceId> {

    /** Баланс конкретного участника в команде. */
    Optional<Balance> findByIdTeamIdAndIdUserId(Long teamId, Long userId);

    /** Все балансы команды. */
    List<Balance> findByIdTeamId(Long teamId);
}