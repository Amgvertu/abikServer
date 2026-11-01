package info.prorabka.abik.server.event.service;

import info.prorabka.abik.server.charge.entity.Charge;
import info.prorabka.abik.server.charge.entity.ChargeDistribution;
import info.prorabka.abik.server.charge.entity.ChargeGroup;
import info.prorabka.abik.server.charge.entity.ChargeStatus;
import info.prorabka.abik.server.charge.entity.ChargeTemplate;
import info.prorabka.abik.server.charge.repository.ChargeGroupRepository;
import info.prorabka.abik.server.charge.repository.ChargeRepository;
import info.prorabka.abik.server.charge.repository.ChargeTemplateRepository;
import info.prorabka.abik.server.common.exception.BadRequestException;
import info.prorabka.abik.server.common.exception.ConflictException;
import info.prorabka.abik.server.common.exception.ForbiddenException;
import info.prorabka.abik.server.common.exception.NotFoundException;
import info.prorabka.abik.server.event.entity.Event;
import info.prorabka.abik.server.event.entity.EventLineup;
import info.prorabka.abik.server.event.entity.EventSection;
import info.prorabka.abik.server.event.entity.EventVote;
import info.prorabka.abik.server.event.entity.VoteAnswer;
import info.prorabka.abik.server.event.repository.EventLineupRepository;
import info.prorabka.abik.server.event.repository.EventRepository;
import info.prorabka.abik.server.event.repository.EventVoteRepository;
import info.prorabka.abik.server.notification.service.NotificationService;
import info.prorabka.abik.server.team.entity.Team;
import info.prorabka.abik.server.team.repository.TeamRepository;
import info.prorabka.abik.server.team.service.TeamMemberService;
import info.prorabka.abik.server.user.entity.UserPosition;
import info.prorabka.abik.server.user.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Начисление за игру (регулярка/двухсторонка) по настройкам события.
 * MANUAL — сумма и распределение из самого события;
 * TEMPLATE — сумма, распределение и получатели из шаблона «Регулярка».
 * «Отметившиеся» = проголосовавшие YES; «заявленные» = игроки в lineup
 * (если заявка пуста — тоже YES).
 */
@Service
public class EventChargeService {

    private final EventRepository eventRepository;
    private final EventVoteRepository voteRepository;
    private final EventLineupRepository lineupRepository;
    private final ChargeRepository chargeRepository;
    private final ChargeGroupRepository groupRepository;
    private final ChargeTemplateRepository templateRepository;
    private final TeamMemberService teamMemberService;
    private final TeamRepository teamRepository;
    private final NotificationService notificationService;
    private final UserProfileRepository profileRepository;

    public EventChargeService(EventRepository eventRepository,
                              EventVoteRepository voteRepository,
                              EventLineupRepository lineupRepository,
                              ChargeRepository chargeRepository,
                              ChargeGroupRepository groupRepository,
                              ChargeTemplateRepository templateRepository,
                              TeamMemberService teamMemberService,
                              TeamRepository teamRepository,
                              NotificationService notificationService,
                              UserProfileRepository profileRepository) {
        this.eventRepository = eventRepository;
        this.voteRepository = voteRepository;
        this.lineupRepository = lineupRepository;
        this.chargeRepository = chargeRepository;
        this.groupRepository = groupRepository;
        this.templateRepository = templateRepository;
        this.teamMemberService = teamMemberService;
        this.teamRepository = teamRepository;
        this.notificationService = notificationService;
        this.profileRepository = profileRepository;
    }

    @Transactional
    public Map<String, Object> chargeForEvent(Long teamId, Long eventId, Long actorUserId) {
        teamMemberService.requireManager(teamId, actorUserId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));
        if (!event.getTeamId().equals(teamId)) {
            throw new ForbiddenException("Событие другой команды");
        }
        if (event.getChargedAt() != null) {
            throw new ConflictException("Начисление по этому событию уже выполнено");
        }

        String mode = event.getChargeMode() == null ? "NONE" : event.getChargeMode();
        if ("NONE".equals(mode)) {
            throw new BadRequestException("Для события не настроено начисление");
        }

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Команда не найдена"));

        List<EventVote> votes = voteRepository.findByEventId(eventId);
        Set<Long> yesVoters = votes.stream()
                .filter(v -> v.getAnswer() == VoteAnswer.YES)
                .map(EventVote::getUserId)
                .collect(Collectors.toSet());

        // Основной состав: первые N проголосовавших «Иду» по времени голоса
        // (зеркалит логику EventService.mainSquadLimit). Вратари всегда
        // в составе — в лимит входят только полевые игроки. Резерв в
        // начислении не участвует — платят только те, кто реально играл.
        Set<Long> mainSquad = yesVoters;
        Integer limit = mainSquadLimit(event);
        if (limit != null) {
            List<EventVote> yesSorted = votes.stream()
                    .filter(v -> v.getAnswer() == VoteAnswer.YES)
                    .sorted(java.util.Comparator.comparing(EventVote::getUpdatedAt))
                    .toList();
            List<EventVote> goalies = yesSorted.stream()
                    .filter(v -> isGoalkeeper(v.getUserId())).toList();
            List<EventVote> field = yesSorted.stream()
                    .filter(v -> !isGoalkeeper(v.getUserId())).toList();
            Set<Long> squad = new java.util.HashSet<>();
            goalies.forEach(v -> squad.add(v.getUserId()));
            field.stream().limit(limit).forEach(v -> squad.add(v.getUserId()));
            mainSquad = squad;
        }

        long amountKopecks;
        boolean split;
        Set<Long> recipients;
        Long groupId;

        if ("TEMPLATE".equals(mode)) {
            if (event.getChargeTemplateId() == null) {
                throw new BadRequestException("Не выбран шаблон начисления");
            }
            ChargeTemplate template = templateRepository.findById(event.getChargeTemplateId())
                    .orElseThrow(() -> new NotFoundException("Шаблон не найден"));
            if (!template.getTeamId().equals(teamId)) {
                throw new ForbiddenException("Шаблон принадлежит другой команде");
            }
            amountKopecks = template.getAmountKopecks();
            split = template.getDistribution() == ChargeDistribution.EQUAL;
            // Начисляем только тем, кто был на игре (основной состав без резерва)
            recipients = template.getMemberIds().stream()
                    .filter(mainSquad::contains)
                    .collect(Collectors.toSet());
            groupId = template.getGroupId();
        } else {
            // MANUAL
            if (event.getChargeAmountKopecks() == null) {
                throw new BadRequestException("Не указана сумма начисления");
            }
            if (event.getChargeDistribution() == null) {
                throw new BadRequestException("Не выбран способ начисления");
            }
            amountKopecks = event.getChargeAmountKopecks();
            String distribution = event.getChargeDistribution();
            split = distribution.startsWith("SPLIT");
            boolean attended = distribution.endsWith("ATTENDED");

            Set<Long> roster = lineupRepository.findByEventId(eventId).stream()
                    .map(EventLineup::getUserId)
                    .collect(Collectors.toSet());
            Set<Long> custom = event.getChargeMemberIds();
            if (custom != null && !custom.isEmpty()) {
                // Явный список получателей, выбранный в форме события
                recipients = custom;
            } else {
                recipients = (attended || roster.isEmpty()) ? mainSquad : roster;
            }

            String groupName = event.getSection() == EventSection.SCRIMMAGE
                    ? "Двухсторонка" : "Регулярка";
            ChargeGroup group = groupRepository.findByTeamIdAndName(teamId, groupName)
                    .orElseGet(() -> groupRepository.save(ChargeGroup.builder()
                            .teamId(teamId).name(groupName).build()));
            groupId = group.getId();
        }

        if (recipients.isEmpty()) {
            throw new BadRequestException("Нет получателей начисления");
        }
        long perPerson = split
                ? Math.round(amountKopecks * 1.0 / recipients.size())
                : amountKopecks;
        if (perPerson <= 0) {
            throw new BadRequestException("Сумма на человека получилась нулевой");
        }

        String description = (event.getTitle() == null || event.getTitle().isBlank()
                ? "Событие" : event.getTitle()) + " " + event.getEventDate();

        for (Long userId : recipients) {
            Charge charge = chargeRepository.save(Charge.builder()
                    .teamId(teamId)
                    .groupId(groupId)
                    .subgroupId(null)
                    .userId(userId)
                    .amountKopecks(perPerson)
                    .paidKopecks(0L)
                    .status(ChargeStatus.ACTIVE)
                    .description(description)
                    .dueDate(event.getEventDate())
                    .build());
            notificationService.notifyNewCharge(userId, teamId, team.getName(),
                    charge.getId(), charge.getAmountKopecks(), description);
        }

        event.setChargedAt(OffsetDateTime.now());
        eventRepository.save(event);

        return Map.of(
                "message", "Начисление выполнено",
                "count", recipients.size(),
                "perPersonKopecks", perPerson);
    }

    /**
     * Лимит основного состава: requiredPlayers для регулярки/двухсторонки,
     * иначе null (лимита нет — все проголосовавшие «Иду» считаются составом).
     */
    private Integer mainSquadLimit(Event e) {
        if (e.getRequiredPlayers() == null || e.getRequiredPlayers() <= 0) return null;
        if (e.getSection() == EventSection.REGULAR || e.getSection() == EventSection.SCRIMMAGE) {
            return e.getRequiredPlayers();
        }
        return null;
    }

    /** Игрок — вратарь (по амплуа в профиле). */
    private boolean isGoalkeeper(Long userId) {
        return profileRepository.findById(userId)
                .map(p -> p.getPositions().contains(UserPosition.GOALKEEPER))
                .orElse(false);
    }
}