package info.prorabka.abik.server.payment.service;

import info.prorabka.abik.server.charge.entity.Charge;
import info.prorabka.abik.server.charge.entity.ChargeStatus;
import info.prorabka.abik.server.charge.repository.ChargeRepository;
import info.prorabka.abik.server.common.exception.BadRequestException;
import info.prorabka.abik.server.common.exception.ForbiddenException;
import info.prorabka.abik.server.common.exception.NotFoundException;
import info.prorabka.abik.server.payment.dto.*;
import info.prorabka.abik.server.payment.entity.Payment;
import info.prorabka.abik.server.payment.entity.PaymentAllocation;
import info.prorabka.abik.server.payment.repository.PaymentAllocationRepository;
import info.prorabka.abik.server.payment.repository.PaymentRepository;
import info.prorabka.abik.server.team.entity.MemberStatus;
import info.prorabka.abik.server.team.entity.Team;
import info.prorabka.abik.server.team.entity.TeamStatus;
import info.prorabka.abik.server.team.repository.TeamMemberRepository;
import info.prorabka.abik.server.team.repository.TeamRepository;
import info.prorabka.abik.server.team.service.TeamMemberService;
import info.prorabka.abik.server.user.repository.UserProfileRepository;
import info.prorabka.abik.server.notification.service.NotificationService;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Сервис платежей.
 * Внесение платежа запускает автораспределение по активным долгам:
 * сначала старые (по due_date), затем по id. Остаток идёт в переплату (баланс).
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository allocationRepository;
    private final ChargeRepository chargeRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberService teamMemberService;
    private final UserProfileRepository userProfileRepository;
    private final BalanceService balanceService;
    private final NotificationService notificationService;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentAllocationRepository allocationRepository,
                          ChargeRepository chargeRepository,
                          TeamMemberRepository teamMemberRepository,
                          TeamRepository teamRepository,
                          TeamMemberService teamMemberService,
                          UserProfileRepository userProfileRepository,
                          BalanceService balanceService,
                          NotificationService notificationService) {
        this.paymentRepository = paymentRepository;
        this.allocationRepository = allocationRepository;
        this.chargeRepository = chargeRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.teamRepository = teamRepository;
        this.teamMemberService = teamMemberService;
        this.userProfileRepository = userProfileRepository;
        this.balanceService = balanceService;
        this.notificationService = notificationService;
    }

    // ============================================================
    // Внесение платежа
    // ============================================================

    @Transactional
    public PaymentResponse create(Long teamId, Long actorUserId, PaymentCreateRequest req) {
        // 1. Права
        teamMemberService.requireManager(teamId, actorUserId);

        // 2. Команда активна?
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Команда не найдена"));
        if (team.getStatus() != TeamStatus.ACTIVE) {
            throw new ForbiddenException("Команда в архиве — платежи невозможны");
        }

        // 3. Получатель — активный участник команды
        if (!teamMemberRepository.existsByTeamIdAndUserIdAndStatus(
                teamId, req.userId(), MemberStatus.ACTIVE)) {
            throw new BadRequestException("Получатель не является активным участником команды");
        }

        // 4. Создаём платёж
        Payment payment = Payment.builder()
                .teamId(teamId)
                .userId(req.userId())
                .amountKopecks(req.amountKopecks())
                .paidAt(req.paidAt() != null ? req.paidAt() : java.time.LocalDate.now())
                .description(trimOrNull(req.description()))
                .createdByUserId(actorUserId)
                .build();
        Payment saved = paymentRepository.save(payment);

        // 5. Автораспределение
        distribute(saved);

        // 6. Пересчёт баланса
        balanceService.recalculate(teamId, req.userId());
        notificationService.notifyPayment(req.userId(), teamId, team.getName(), req.amountKopecks(), "Владелец/модератор");
        log.info("Платёж внесён: teamId={}, userId={}, amount={} копеек, by={}",
                teamId, req.userId(), req.amountKopecks(), actorUserId);

        return toResponse(saved);
    }

    // ============================================================
    // Автораспределение
    // ============================================================

    /**
     * Распределить сумму платежа по активным долгам получателя.
     * Сортировка: due_date ASC, id ASC (сначала старые).
     * Остаток не уходит никуда — он остаётся в балансе как переплата.
     */
    @Transactional
    protected void distribute(Payment payment) {
        long remaining = payment.getAmountKopecks();

        // Активные долги получателя — уже отсортированы
        List<Charge> debts = chargeRepository
                .findByTeamIdAndUserIdAndStatusInOrderByDueDateAscIdAsc(
                        payment.getTeamId(),
                        payment.getUserId(),
                        List.of(ChargeStatus.ACTIVE, ChargeStatus.PARTIALLY_PAID));

        for (Charge charge : debts) {
            if (remaining <= 0) break;

            long needed = charge.remainingKopecks();
            if (needed <= 0) continue;

            long toApply = Math.min(remaining, needed);

            // Создаём аллокацию
            PaymentAllocation allocation = PaymentAllocation.builder()
                    .paymentId(payment.getId())
                    .chargeId(charge.getId())
                    .amountKopecks(toApply)
                    .build();
            allocationRepository.save(allocation);

            // Обновляем начисление
            charge.setPaidKopecks(charge.getPaidKopecks() + toApply);
            if (charge.getPaidKopecks() >= charge.getAmountKopecks()) {
                charge.setStatus(ChargeStatus.PAID);
            } else {
                charge.setStatus(ChargeStatus.PARTIALLY_PAID);
            }
            chargeRepository.save(charge);

            remaining -= toApply;
        }

        if (remaining > 0) {
            log.debug("Платёж {}: остаток {} копеек ушёл в переплату",
                    payment.getId(), remaining);
        }
    }

    /**
     * Откатить все аллокации платежа.
     * Используется при редактировании и удалении платежа.
     */
    @Transactional
    protected void revertAllocations(Payment payment) {
        List<PaymentAllocation> allocations = allocationRepository.findByPaymentId(payment.getId());

        for (PaymentAllocation a : allocations) {
            Charge charge = chargeRepository.findById(a.getChargeId()).orElse(null);
            if (charge == null) continue;
            // Отменённые начисления не трогаем — их paid уже обнулён
            if (charge.getStatus() == ChargeStatus.CANCELLED
                    || charge.getStatus() == ChargeStatus.ARCHIVED) {
                continue;
            }

            long newPaid = Math.max(0, charge.getPaidKopecks() - a.getAmountKopecks());
            charge.setPaidKopecks(newPaid);

            if (newPaid == 0) {
                charge.setStatus(ChargeStatus.ACTIVE);
            } else if (newPaid < charge.getAmountKopecks()) {
                charge.setStatus(ChargeStatus.PARTIALLY_PAID);
            } else {
                charge.setStatus(ChargeStatus.PAID);
            }
            chargeRepository.save(charge);
        }

        allocationRepository.deleteByPaymentId(payment.getId());
    }

    // ============================================================
    // Редактирование
    // ============================================================

    @Transactional
    public PaymentResponse update(Long teamId, Long paymentId, Long actorUserId,
                                  PaymentUpdateRequest req) {
        teamMemberService.requireManager(teamId, actorUserId);

        Payment payment = getActiveOrThrow(paymentId);
        if (!payment.getTeamId().equals(teamId)) {
            throw new ForbiddenException("Платёж принадлежит другой команде");
        }

        Long oldUserId = payment.getUserId();
        Long newUserId = req.userId() != null ? req.userId() : oldUserId;

        // Если пользователь меняется — проверим, что новый получатель активен
        if (!newUserId.equals(oldUserId)) {
            if (!teamMemberRepository.existsByTeamIdAndUserIdAndStatus(
                    teamId, newUserId, MemberStatus.ACTIVE)) {
                throw new BadRequestException("Новый получатель не является активным участником");
            }
        }

        // Откатываем старые аллокации (по старому получателю)
        revertAllocations(payment);

        payment.setUserId(newUserId);
        payment.setAmountKopecks(req.amountKopecks());
        if (req.paidAt() != null) {
            payment.setPaidAt(req.paidAt());
        }
        payment.setDescription(trimOrNull(req.description()));

        Payment saved = paymentRepository.save(payment);

        // Пересобираем распределение по новому получателю
        distribute(saved);

        // Пересчитать балансы обоих пользователей
        balanceService.recalculate(teamId, newUserId);
        if (!oldUserId.equals(newUserId)) {
            balanceService.recalculate(teamId, oldUserId);
        }

        log.info("Платёж обновлён: paymentId={}, сумма={}, userId={}",
                paymentId, req.amountKopecks(), newUserId);
        return toResponse(saved);
    }

    // ============================================================
    // Удаление (soft)
    // ============================================================

    @Transactional
    public void delete(Long teamId, Long paymentId, Long actorUserId) {
        teamMemberService.requireManager(teamId, actorUserId);

        Payment payment = getActiveOrThrow(paymentId);
        if (!payment.getTeamId().equals(teamId)) {
            throw new ForbiddenException("Платёж принадлежит другой команде");
        }

        // Откатываем аллокации — долги возвращаются в исходное состояние
        revertAllocations(payment);

        payment.setDeletedAt(OffsetDateTime.now());
        paymentRepository.save(payment);

        balanceService.recalculate(teamId, payment.getUserId());
        log.info("Платёж удалён (soft): paymentId={}, by={}", paymentId, actorUserId);
    }

    // ============================================================
    // Чтение
    // ============================================================

    public List<PaymentResponse> list(Long teamId, Long actorUserId, PaymentFilterRequest filter) {
        teamMemberService.requireMember(teamId, actorUserId);

        List<Payment> payments;
        if (filter.userId() != null) {
            payments = paymentRepository
                    .findByTeamIdAndUserIdAndDeletedAtIsNullOrderByPaidAtDescIdDesc(teamId, filter.userId());
        } else {
            payments = paymentRepository
                    .findByTeamIdAndDeletedAtIsNullOrderByPaidAtDescIdDesc(teamId);
        }

        return payments.stream()
                .filter(p -> filter.dateFrom() == null
                        || !p.getPaidAt().isBefore(filter.dateFrom()))
                .filter(p -> filter.dateTo() == null
                        || !p.getPaidAt().isAfter(filter.dateTo()))
                .filter(p -> filter.amountMinKopecks() == null
                        || p.getAmountKopecks() >= filter.amountMinKopecks())
                .filter(p -> filter.amountMaxKopecks() == null
                        || p.getAmountKopecks() <= filter.amountMaxKopecks())
                .map(this::toResponse)
                .toList();
    }

    public PaymentResponse get(Long teamId, Long paymentId, Long actorUserId) {
        teamMemberService.requireMember(teamId, actorUserId);
        Payment payment = getActiveOrThrow(paymentId);
        if (!payment.getTeamId().equals(teamId)) {
            throw new ForbiddenException("Платёж принадлежит другой команде");
        }
        return toResponse(payment);
    }

    // ============================================================
    // Helpers
    // ============================================================

    private Payment getActiveOrThrow(Long paymentId) {
        return paymentRepository.findByIdAndDeletedAtIsNull(paymentId)
                .orElseThrow(() -> new NotFoundException("Платёж не найден"));
    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private PaymentResponse toResponse(Payment p) {
        String userLastName = null, userFirstName = null;
        var userProfile = userProfileRepository.findById(p.getUserId());
        if (userProfile.isPresent()) {
            userLastName = userProfile.get().getLastName();
            userFirstName = userProfile.get().getFirstName();
        }

        String byLastName = null, byFirstName = null;
        var byProfile = userProfileRepository.findById(p.getCreatedByUserId());
        if (byProfile.isPresent()) {
            byLastName = byProfile.get().getLastName();
            byFirstName = byProfile.get().getFirstName();
        }

        return PaymentResponse.from(p, userLastName, userFirstName, byLastName, byFirstName);
    }

    // ============================================================
    // Перераспределение после отмены начисления
    // ============================================================

    /**
     * Вызывается после того, как начисление помечено CANCELLED и его paidKopecks обнулён.
     *
     * Что делает:
     *  1. Собирает все аллокации отменённого начисления.
     *  2. Группирует их по payment_id (знаем, сколько какой платёж «дал» на отменённый долг).
     *  3. Удаляет старые аллокации.
     *  4. Пытается «переразмазать» освободившиеся суммы по оставшимся долгам получателя
     *     (в том же порядке: due_date ASC, id ASC).
     *  5. Что не влезло — остаётся в переплате (баланс сам это покажет).
     *
     * Привязка к исходным платежам сохраняется: если позже платёж удалят/отредактируют,
     * откат сработает корректно.
     */
    @Transactional
    public void reallocateAfterChargeCancel(Charge cancelledCharge) {
        List<PaymentAllocation> allocations = allocationRepository
                .findByChargeId(cancelledCharge.getId());

        if (allocations.isEmpty()) {
            // Ничего не было оплачено — просто пересчитать баланс
            balanceService.recalculate(cancelledCharge.getTeamId(), cancelledCharge.getUserId());
            return;
        }

        // 1. Собираем суммы по платежам
        Map<Long, Long> byPayment = new HashMap<>();
        for (PaymentAllocation a : allocations) {
            byPayment.merge(a.getPaymentId(), a.getAmountKopecks(), Long::sum);
        }

        // 2. Удаляем аллокации отменённого charge
        allocationRepository.deleteByChargeId(cancelledCharge.getId());

        // 3. Оставшиеся активные долги получателя (исключая сам отменённый — он теперь CANCELLED)
        List<Charge> remainingDebts = chargeRepository
                .findByTeamIdAndUserIdAndStatusInOrderByDueDateAscIdAsc(
                        cancelledCharge.getTeamId(),
                        cancelledCharge.getUserId(),
                        List.of(ChargeStatus.ACTIVE, ChargeStatus.PARTIALLY_PAID));

        // 4. Перераспределяем
        for (Map.Entry<Long, Long> entry : byPayment.entrySet()) {
            Long paymentId = entry.getKey();
            long leftover = entry.getValue();

            for (Charge debt : remainingDebts) {
                if (leftover <= 0) break;
                long needed = debt.remainingKopecks();
                if (needed <= 0) continue;
                long toApply = Math.min(leftover, needed);

                allocationRepository.save(PaymentAllocation.builder()
                        .paymentId(paymentId)
                        .chargeId(debt.getId())
                        .amountKopecks(toApply)
                        .build());

                debt.setPaidKopecks(debt.getPaidKopecks() + toApply);
                debt.setStatus(debt.getPaidKopecks() >= debt.getAmountKopecks()
                        ? ChargeStatus.PAID
                        : ChargeStatus.PARTIALLY_PAID);
                chargeRepository.save(debt);

                leftover -= toApply;
            }

            if (leftover > 0) {
                log.debug("Платёж {}: {} копеек ушли в переплату после отмены chargeId={}",
                        paymentId, leftover, cancelledCharge.getId());
            }
        }

        // 5. Пересчёт баланса
        balanceService.recalculate(cancelledCharge.getTeamId(), cancelledCharge.getUserId());
        log.info("Перераспределено после отмены начисления: chargeId={}, затронуто платежей={}",
                cancelledCharge.getId(), byPayment.size());
    }
}