package info.prorabka.abik.server.charge.repository;

import info.prorabka.abik.server.charge.entity.Charge;
import info.prorabka.abik.server.charge.entity.ChargeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChargeRepository extends JpaRepository<Charge, Long> {

    /** Начисления команды, свежие сверху. */
    List<Charge> findByTeamIdOrderByCreatedAtDesc(Long teamId);

    /** Начисления команды по сроку оплаты: ближайшие сверху, затем по id. */
    List<Charge> findByTeamIdOrderByDueDateAscIdAsc(Long teamId);

    /** Начисления пользователя в команде. */
    List<Charge> findByTeamIdAndUserIdOrderByCreatedAtDesc(Long teamId, Long userId);

    /** Начисления пользователя в команде с определённым статусом. */
    List<Charge> findByTeamIdAndUserIdAndStatusOrderByCreatedAtDesc(
            Long teamId, Long userId, ChargeStatus status);

    /** Проверка: не сгенерировано ли уже начисление по шаблону за период. */
    Optional<Charge> findByTemplateIdAndUserIdAndPeriodYearAndPeriodMonth(
            Long templateId, Long userId, Short year, Short month);

    /** Все активные начисления пользователя (для распределения платежа). */
    List<Charge> findByTeamIdAndUserIdAndStatusInOrderByDueDateAscIdAsc(
            Long teamId, Long userId, List<ChargeStatus> statuses);
}