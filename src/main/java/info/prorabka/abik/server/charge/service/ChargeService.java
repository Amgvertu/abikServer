package info.prorabka.abik.server.charge.service;

import info.prorabka.abik.server.charge.dto.*;
import info.prorabka.abik.server.charge.entity.*;
import info.prorabka.abik.server.charge.repository.ChargeRepository;
import info.prorabka.abik.server.charge.repository.ChargeSubgroupRepository;
import info.prorabka.abik.server.common.exception.BadRequestException;
import info.prorabka.abik.server.common.exception.ForbiddenException;
import info.prorabka.abik.server.common.exception.NotFoundException;
import info.prorabka.abik.server.notification.service.NotificationService;
import info.prorabka.abik.server.payment.service.PaymentService;
import info.prorabka.abik.server.team.entity.MemberStatus;
import info.prorabka.abik.server.team.entity.Team;
import info.prorabka.abik.server.team.entity.TeamStatus;
import info.prorabka.abik.server.team.repository.TeamMemberRepository;
import info.prorabka.abik.server.team.repository.TeamRepository;
import info.prorabka.abik.server.team.service.TeamMemberService;
import info.prorabka.abik.server.user.entity.User;
import info.prorabka.abik.server.user.repository.UserProfileRepository;
import info.prorabka.abik.server.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import info.prorabka.abik.server.charge.repository.ChargeTemplateRepository;
import info.prorabka.abik.server.geo.entity.Region;
import info.prorabka.abik.server.geo.repository.RegionRepository;
import info.prorabka.abik.server.team.repository.TeamRepository;

import java.time.ZoneId;
import java.time.ZonedDateTime;


import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Сервис начислений.
 * Отвечает за создание вручную, чтение, фильтрацию, отмену.
 * Генерация из шаблонов — в отдельном методе, вызываемом планировщиком.
 */
@Service
public class ChargeService {

    private static final Logger log = LoggerFactory.getLogger(ChargeService.class);

    /** Округление суммы вверх — по ТЗ до 100 ₽ = 10000 копеек. */
    private static final long ROUNDING_KOPECKS = 10000L;

    private final ChargeRepository chargeRepository;
    private final ChargeSubgroupRepository subgroupRepository;
    private final ChargeGroupService groupService;
    private final TeamMemberService teamMemberService;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final ChargeTemplateRepository templateRepository;
    private final RegionRepository regionRepository;
    private final PaymentService paymentService;
    private final NotificationService notificationService;


    public ChargeService(ChargeRepository chargeRepository,
                         ChargeSubgroupRepository subgroupRepository,
                         ChargeGroupService groupService,
                         TeamMemberService teamMemberService,
                         TeamMemberRepository teamMemberRepository,
                         TeamRepository teamRepository,
                         UserRepository userRepository,
                         UserProfileRepository userProfileRepository,
                         ChargeTemplateRepository templateRepository,
                         RegionRepository regionRepository,
                         PaymentService paymentService,
                         NotificationService notificationService) {          // ⬅️ новый параметр
        this.chargeRepository = chargeRepository;
        this.subgroupRepository = subgroupRepository;
        this.groupService = groupService;
        this.teamMemberService = teamMemberService;
        this.teamMemberRepository = teamMemberRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.templateRepository = templateRepository;
        this.regionRepository = regionRepository;
        this.paymentService = paymentService;
        this.notificationService = notificationService;// ⬅️ новая строка
    }

    // ============================================================
    // Создание разового начисления вручную
    // ============================================================

    @Transactional
    public List<ChargeResponse> createManual(Long teamId, Long actorUserId,
                                             ChargeCreateManualRequest req) {
        teamMemberService.requireManager(teamId, actorUserId);

        Team team = getTeamOrThrow(teamId);
        if (team.getStatus() != TeamStatus.ACTIVE) {
            throw new ForbiddenException("Команда в архиве — начисления невозможны");
        }

        // Проверка группы
        ChargeGroup group = groupService.getGroupOrThrow(req.groupId());
        if (!group.getTeamId().equals(teamId)) {
            throw new ForbiddenException("Группа принадлежит другой команде");
        }

        // Проверка подгруппы
        ChargeSubgroup subgroup = null;
        if (req.subgroupId() != null) {
            subgroup = groupService.getSubgroupOrThrow(req.subgroupId());
            if (!subgroup.getGroupId().equals(group.getId())) {
                throw new BadRequestException("Подгруппа не принадлежит указанной группе");
            }
        }

        // Проверка, что все userId — активные участники
        Set<Long> activeIds = teamMemberRepository
                .findByTeamIdAndStatusOrderByJoinedAtAsc(teamId, MemberStatus.ACTIVE)
                .stream()
                .map(m -> m.getUserId())
                .collect(Collectors.toSet());

        for (Long userId : req.userIds()) {
            if (!activeIds.contains(userId)) {
                throw new BadRequestException(
                        "Пользователь " + userId + " не является активным членом команды");
            }
        }

        // Делим сумму поровну, округляя вверх до 100 ₽
        /*long amountPerUser = roundUpTo100Rub(req.amountKopecks() / req.userIds().size());
        if (amountPerUser <= 0) {
            amountPerUser = ROUNDING_KOPECKS; // минимум 100 ₽
        }*/

        // По ТЗ (6.2): введённая сумма назначается КАЖДОМУ выбранному участнику
        // целиком — не делится между ними и не округляется
        long amountPerUser = req.amountKopecks();

        List<Charge> created = new ArrayList<>();
        for (Long userId : req.userIds()) {
            Charge charge = Charge.builder()
                    .teamId(teamId)
                    .groupId(group.getId())
                    .subgroupId(subgroup != null ? subgroup.getId() : null)
                    .userId(userId)
                    .amountKopecks(amountPerUser)
                    .paidKopecks(0L)
                    .status(ChargeStatus.ACTIVE)
                    .description(trimOrNull(req.description()))
                    .dueDate(req.dueDate())
                    .build();
            created.add(chargeRepository.save(charge));
            notificationService.notifyNewCharge(userId, teamId, team.getName(),
                    charge.getId(), charge.getAmountKopecks(), charge.getDescription());
        }

        log.info("Создано {} разовых начислений: teamId={}, actor={}, сумма/чел={} копеек",
                created.size(), teamId, actorUserId, amountPerUser);

        return created.stream().map(this::toResponse).toList();
    }

    // ============================================================
    // Чтение и фильтрация
    // ============================================================

    /**
     * Список начислений команды с фильтрами.
     * Доступно активным участникам.
     */
    public List<ChargeResponse> listCharges(Long teamId, Long actorUserId, ChargeFilterRequest filter) {
        teamMemberService.requireMember(teamId, actorUserId);

        List<Charge> charges = chargeRepository.findByTeamIdOrderByDueDateAscIdAsc(teamId);

        return charges.stream()
                .filter(c -> filter.userId() == null || c.getUserId().equals(filter.userId()))
                .filter(c -> filter.groupId() == null || c.getGroupId().equals(filter.groupId()))
                .filter(c -> filter.subgroupId() == null || filter.subgroupId().equals(c.getSubgroupId()))
                .filter(c -> filter.status() == null || c.getStatus() == filter.status())
                .filter(c -> filter.dateFrom() == null || !c.getDueDate().isBefore(filter.dateFrom()))
                .filter(c -> filter.dateTo() == null || !c.getDueDate().isAfter(filter.dateTo()))
                .filter(c -> filter.amountMinKopecks() == null
                        || c.getAmountKopecks() >= filter.amountMinKopecks())
                .filter(c -> filter.amountMaxKopecks() == null
                        || c.getAmountKopecks() <= filter.amountMaxKopecks())
                .map(this::toResponse)
                .toList();
    }

    /** Одно начисление. Доступно активным участникам. */
    public ChargeResponse getCharge(Long teamId, Long chargeId, Long actorUserId) {
        teamMemberService.requireMember(teamId, actorUserId);
        Charge c = getChargeOrThrow(chargeId);
        if (!c.getTeamId().equals(teamId)) {
            throw new ForbiddenException("Начисление принадлежит другой команде");
        }
        return toResponse(c);
    }

    // ============================================================
    // Отмена
    // ============================================================

    /**
     * Отменить начисление.
     * Если уже были оплаты — по ТЗ «перераспределение на другие долги, остаток в баланс».
     * Реализация распределения — в модуле payment. Здесь только помечаем CANCELLED,
     * чтобы payment при следующем платеже перераспределил оплаченное.
     */
    @Transactional
    public ChargeResponse cancel(Long teamId, Long chargeId, Long actorUserId,
                                 ChargeCancelRequest req) {
        teamMemberService.requireManager(teamId, actorUserId);

        Charge charge = getChargeOrThrow(chargeId);
        if (!charge.getTeamId().equals(teamId)) {
            throw new ForbiddenException("Начисление принадлежит другой команде");
        }
        if (charge.getStatus() == ChargeStatus.CANCELLED) {
            throw new BadRequestException("Начисление уже отменено");
        }
        if (charge.getStatus() == ChargeStatus.ARCHIVED) {
            throw new BadRequestException("Начисление в архиве");
        }

        // Помечаем CANCELLED и обнуляем paid, чтобы долг «исчез» из списка активных
        charge.setStatus(ChargeStatus.CANCELLED);
        charge.setPaidKopecks(0L);
        charge.setCancelledAt(OffsetDateTime.now());
        charge.setCancelReason(req.reason().trim());
        Charge saved = chargeRepository.save(charge);

        // Перераспределяем «освободившиеся» деньги на оставшиеся долги
        paymentService.reallocateAfterChargeCancel(saved);

        log.info("Начисление отменено: chargeId={}, actor={}, reason={}",
                chargeId, actorUserId, req.reason());
        return toResponse(saved);
    }

    // ============================================================
    // Helpers
    // ============================================================

    public Charge getChargeOrThrow(Long chargeId) {
        return chargeRepository.findById(chargeId)
                .orElseThrow(() -> new NotFoundException("Начисление не найдено"));
    }

    private Team getTeamOrThrow(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Команда не найдена"));
    }

    /**
     * Округление вверх до ближайших 100 ₽ (10000 копеек).
     * Пример: 12345 → 20000; 10000 → 10000; 10001 → 20000.
     */
    static long roundUpTo100Rub(long kopecks) {
        if (kopecks <= 0) return 0;
        return ((kopecks + ROUNDING_KOPECKS - 1) / ROUNDING_KOPECKS) * ROUNDING_KOPECKS;
    }

    private String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** Собрать ChargeResponse, подтянув имена группы, подгруппы и ФИО user. */
    private ChargeResponse toResponse(Charge c) {
        String groupName = groupService.getGroupOrThrow(c.getGroupId()).getName();

        String subgroupName = null;
        if (c.getSubgroupId() != null) {
            subgroupName = subgroupRepository.findById(c.getSubgroupId())
                    .map(ChargeSubgroup::getName)
                    .orElse(null);
        }

        String lastName = null;
        String firstName = null;
        var profileOpt = userProfileRepository.findById(c.getUserId());
        if (profileOpt.isPresent()) {
            lastName = profileOpt.get().getLastName();
            firstName = profileOpt.get().getFirstName();
        }

        return ChargeResponse.from(c, groupName, subgroupName, lastName, firstName);
    }

    // ============================================================
    // Генерация из шаблонов (вызывается планировщиком)
    // ============================================================

    /**
     * Сгенерировать начисления по всем активным систематическим шаблонам,
     * у которых наступил день генерации.
     *
     * Алгоритм:
     *  1. Для каждой активной команды определяем локальную дату по её timezone.
     *  2. Для каждого активного SYSTEMATIC-шаблона проверяем:
     *       - today.dayOfMonth >= dayOfMonth,
     *       - today в диапазоне [startDate, endDate] (если заданы).
     *  3. Для каждого memberId шаблона:
     *       - пропускаем, если участник уже не активен;
     *       - пропускаем, если начисление за этот период уже есть.
     *  4. Создаём Charge с period_year / period_month.
     *
     * Метод идемпотентен: повторный запуск не создаст дублей (защита на уровне БД).
     *
     * @return количество созданных начислений
     */
    @Transactional
    public int generateFromTemplates(LocalDate today) {
        List<Team> activeTeams = teamRepository.findByStatusOrderByNameAsc(TeamStatus.ACTIVE);
        int totalCreated = 0;

        for (Team team : activeTeams) {
            try {
                totalCreated += generateForTeam(team, today);
            } catch (Exception e) {
                log.error("Ошибка генерации для команды teamId={}: {}",
                        team.getId(), e.getMessage(), e);
                // не падаем — продолжаем со следующей командой
            }
        }

        if (totalCreated > 0) {
            log.info("Сгенерировано начислений: {}", totalCreated);
        }
        return totalCreated;
    }

    /** Генерация для одной команды. */
    private int generateForTeam(Team team, LocalDate today) {
        // Локальная дата команды
        LocalDate localToday = today;
        if (team.getHomeRegionId() != null) {
            Region region = regionRepository.findById(team.getHomeRegionId()).orElse(null);
            if (region != null && region.getTimezone() != null) {
                ZoneId zone = ZoneId.of(region.getTimezone());
                localToday = ZonedDateTime.now(zone).toLocalDate();
            }
        }

        List<ChargeTemplate> templates = templateRepository
                .findByTeamIdAndActiveOrderByCreatedAtDesc(team.getId(), true);

        // Активные участники команды — чтобы отфильтровать исключённых из шаблона
        Set<Long> activeUserIds = teamMemberRepository
                .findByTeamIdAndStatusOrderByJoinedAtAsc(team.getId(), MemberStatus.ACTIVE)
                .stream()
                .map(m -> m.getUserId())
                .collect(Collectors.toSet());

        int created = 0;
        for (ChargeTemplate template : templates) {
            if (template.getType() != ChargeType.SYSTEMATIC) continue;
            if (template.getDayOfMonth() == null) continue;

            // Проверка окна действия
            if (template.getStartDate() != null && localToday.isBefore(template.getStartDate())) {
                continue;
            }
            if (template.getEndDate() != null && localToday.isAfter(template.getEndDate())) {
                continue;
            }

            // Проверка дня месяца: генерируем, если локальный день >= dayOfMonth,
            // а за текущий период начисления ещё нет. Это позволяет догонять пропущенные дни.
            if (localToday.getDayOfMonth() < template.getDayOfMonth()) {
                continue;
            }

            short periodYear = (short) localToday.getYear();
            short periodMonth = (short) localToday.getMonthValue();

            // Дата начисления
            LocalDate dueDate = LocalDate.of(
                    localToday.getYear(),
                    localToday.getMonth(),
                    Math.min(template.getDayOfMonth(), localToday.lengthOfMonth())
            );

            // Делим сумму, если EQUAL
            long amountPerUser = template.getAmountKopecks();
            if (template.getDistribution() == ChargeDistribution.EQUAL) {
                amountPerUser = roundUpTo100Rub(amountPerUser / template.getMemberIds().size());
                if (amountPerUser <= 0) {
                    amountPerUser = ROUNDING_KOPECKS;
                }
            }

            for (Long userId : template.getMemberIds()) {
                // Уже не активный участник — пропускаем
                if (!activeUserIds.contains(userId)) continue;

                // Проверка на дубль
                boolean exists = chargeRepository
                        .findByTemplateIdAndUserIdAndPeriodYearAndPeriodMonth(
                                template.getId(), userId, periodYear, periodMonth)
                        .isPresent();
                if (exists) continue;

                Charge charge = Charge.builder()
                        .teamId(team.getId())
                        .templateId(template.getId())
                        .groupId(template.getGroupId())
                        .subgroupId(template.getSubgroupId())
                        .userId(userId)
                        .amountKopecks(amountPerUser)
                        .paidKopecks(0L)
                        .status(ChargeStatus.ACTIVE)
                        .description(template.getName())
                        .dueDate(dueDate)
                        .periodYear(periodYear)
                        .periodMonth(periodMonth)
                        .build();

                chargeRepository.save(charge);

                // ВОТ ОНО, УВЕДОМЛЕНИЕ:
                notificationService.notifyNewCharge(userId, team.getId(), team.getName(),
                        charge.getId(), charge.getAmountKopecks(), charge.getDescription());

                created++;
            }
        }

        if (created > 0) {
            log.info("Команда teamId={}: сгенерировано {} начислений за {}",
                    team.getId(), created, localToday);
        }
        return created;
    }
}