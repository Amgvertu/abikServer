package info.prorabka.abik.server.payment.service;

import info.prorabka.abik.server.charge.entity.Charge;
import info.prorabka.abik.server.charge.entity.ChargeStatus;
import info.prorabka.abik.server.charge.repository.ChargeRepository;
import info.prorabka.abik.server.common.exception.BadRequestException;
import info.prorabka.abik.server.payment.dto.BalanceAdjustmentRequest;
import info.prorabka.abik.server.payment.dto.BalanceAdjustmentResponse;
import info.prorabka.abik.server.payment.dto.BalanceResponse;
import info.prorabka.abik.server.payment.entity.Balance;
import info.prorabka.abik.server.payment.entity.BalanceAdjustment;
import info.prorabka.abik.server.payment.entity.BalanceId;
import info.prorabka.abik.server.payment.repository.BalanceAdjustmentRepository;
import info.prorabka.abik.server.payment.repository.BalanceRepository;
import info.prorabka.abik.server.payment.repository.PaymentRepository;
import info.prorabka.abik.server.team.entity.MemberStatus;
import info.prorabka.abik.server.team.repository.TeamMemberRepository;
import info.prorabka.abik.server.team.service.TeamMemberService;
import info.prorabka.abik.server.user.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Сервис баланса участников.
 *
 * Баланс = SUM(активные платежи) − SUM(активные начисления).
 *  - Положительный — переплата (деньги в команде «в запасе»).
 *  - Отрицательный — долг.
 *  - Ноль — всё закрыто.
 *
 * Значение кешируется в таблице balances и пересчитывается
 * при любом изменении платежей или начислений.
 */
@Service
public class BalanceService {

    private static final Logger log = LoggerFactory.getLogger(BalanceService.class);

    private final BalanceRepository balanceRepository;
    private final PaymentRepository paymentRepository;
    private final ChargeRepository chargeRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamMemberService teamMemberService;
    private final UserProfileRepository userProfileRepository;
    private final BalanceAdjustmentRepository adjustmentRepository;

    public BalanceService(BalanceRepository balanceRepository,
                          PaymentRepository paymentRepository,
                          ChargeRepository chargeRepository,
                          TeamMemberRepository teamMemberRepository,
                          TeamMemberService teamMemberService,
                          UserProfileRepository userProfileRepository,
                          BalanceAdjustmentRepository adjustmentRepository) {
        this.balanceRepository = balanceRepository;
        this.paymentRepository = paymentRepository;
        this.chargeRepository = chargeRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.teamMemberService = teamMemberService;
        this.userProfileRepository = userProfileRepository;
        this.adjustmentRepository = adjustmentRepository;
    }

    // ============================================================
    // Пересчёт
    // ============================================================

    /**
     * Пересчитать баланс участника и сохранить в кеш.
     * Вызывается при любом изменении платежей или начислений.
     */
    @Transactional
    public Balance recalculate(Long teamId, Long userId) {
        long paid = paymentRepository.sumActiveByTeamAndUser(teamId, userId);
        long due = sumActiveCharges(teamId, userId);
        long adjustments = adjustmentRepository.sumByTeamAndUser(teamId, userId);
        long balance = paid - due + adjustments;

        BalanceId id = new BalanceId(teamId, userId);
        Balance entity = balanceRepository.findById(id).orElseGet(() ->
                Balance.builder()
                        .id(id)
                        .amountKopecks(0L)
                        .build());
        entity.setAmountKopecks(balance);

        Balance saved = balanceRepository.save(entity);
        log.debug("Баланс пересчитан: teamId={}, userId={}, paid={}, due={}, balance={}",
                teamId, userId, paid, due, balance);
        return saved;
    }

    /**
     * Пересчитать балансы всех активных участников команды.
     * Используется, например, при массовой отмене начислений или архивации.
     */
    @Transactional
    public int recalculateAll(Long teamId) {
        var members = teamMemberRepository
                .findByTeamIdAndStatusOrderByJoinedAtAsc(teamId, MemberStatus.ACTIVE);
        int count = 0;
        for (var m : members) {
            recalculate(teamId, m.getUserId());
            count++;
        }
        return count;
    }

    // ============================================================
    // Чтение
    // ============================================================

    /** Баланс конкретного участника. Пересчитываем перед выдачей — на всякий случай. */
    @Transactional
    public BalanceResponse getBalance(Long teamId, Long targetUserId, Long actorUserId) {
        teamMemberService.requireMember(teamId, actorUserId);

        Balance balance = recalculate(teamId, targetUserId);

        String lastName = null;
        String firstName = null;
        var profileOpt = userProfileRepository.findById(targetUserId);
        if (profileOpt.isPresent()) {
            lastName = profileOpt.get().getLastName();
            firstName = profileOpt.get().getFirstName();
        }

        return new BalanceResponse(
                balance.getId().getTeamId(),
                balance.getId().getUserId(),
                lastName,
                firstName,
                balance.getAmountKopecks(),
                balance.getUpdatedAt()
        );
    }

    /** Баланс всех участников команды. */
    @Transactional
    public List<BalanceResponse> getAllBalances(Long teamId, Long actorUserId) {
        teamMemberService.requireMember(teamId, actorUserId);

        var members = teamMemberRepository
                .findByTeamIdAndStatusOrderByJoinedAtAsc(teamId, MemberStatus.ACTIVE);

        return members.stream()
                .map(m -> {
                    Balance b = recalculate(teamId, m.getUserId());
                    String lastName = null;
                    String firstName = null;
                    var profileOpt = userProfileRepository.findById(m.getUserId());
                    if (profileOpt.isPresent()) {
                        lastName = profileOpt.get().getLastName();
                        firstName = profileOpt.get().getFirstName();
                    }
                    return new BalanceResponse(
                            b.getId().getTeamId(),
                            b.getId().getUserId(),
                            lastName,
                            firstName,
                            b.getAmountKopecks(),
                            b.getUpdatedAt()
                    );
                })
                .toList();
    }

    // ============================================================
    // Helpers
    // ============================================================

    /**
     * Сумма активных долгов пользователя.
     * ACTIVE и PARTIALLY_PAID — считаются долгом.
     * PAID, CANCELLED, ARCHIVED — не считаются.
     */
    private long sumActiveCharges(Long teamId, Long userId) {
        List<Charge> charges = chargeRepository
                .findByTeamIdAndUserIdAndStatusInOrderByDueDateAscIdAsc(
                        teamId, userId,
                        List.of(ChargeStatus.ACTIVE, ChargeStatus.PARTIALLY_PAID));

        long total = 0;
        for (Charge c : charges) {
            total += c.remainingKopecks();
        }
        return total;
    }

    /** Используется в PaymentService — вернуть «сырое» значение баланса. */
    @Transactional
    public long getRawBalance(Long teamId, Long userId) {
        long paid = paymentRepository.sumActiveByTeamAndUser(teamId, userId);
        long due = sumActiveCharges(teamId, userId);
        long adjustments = adjustmentRepository.sumByTeamAndUser(teamId, userId);
        return paid - due + adjustments;
    }

    // ============================================================
    // Корректировки баланса
    // ============================================================

    @Transactional
    public BalanceAdjustmentResponse adjust(Long teamId, Long userId, Long actorUserId, BalanceAdjustmentRequest req) {
        // Проверяем, что пользователь в команде
        if (!teamMemberRepository.existsByTeamIdAndUserIdAndStatus(teamId, userId, MemberStatus.ACTIVE)) {
            throw new BadRequestException("Пользователь не является активным участником команды");
        }

        BalanceAdjustment adjustment = BalanceAdjustment.builder()
                .teamId(teamId)
                .userId(userId)
                .amountKopecks(req.amountKopecks())
                .reason(req.reason().trim())
                .createdByUserId(actorUserId)
                .build();
        BalanceAdjustment saved = adjustmentRepository.save(adjustment);

        // Сразу пересчитываем баланс
        recalculate(teamId, userId);

        return BalanceAdjustmentResponse.from(saved);
    }

    public List<BalanceAdjustmentResponse> getAdjustmentHistory(Long teamId, Long userId) {
        return adjustmentRepository.findByTeamIdAndUserIdOrderByCreatedAtDesc(teamId, userId).stream()
                .map(BalanceAdjustmentResponse::from)
                .toList();
    }
}