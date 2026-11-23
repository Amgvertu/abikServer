package info.prorabka.abik.server.charge.service;

import info.prorabka.abik.server.charge.dto.ActiveSubscriptionResponse;
import info.prorabka.abik.server.charge.dto.SubscriptionPollResponse;
import info.prorabka.abik.server.charge.dto.SubscriptionPollVoteRequest;
import info.prorabka.abik.server.charge.entity.*;
import info.prorabka.abik.server.charge.repository.ChargeRepository;
import info.prorabka.abik.server.charge.repository.ChargeTemplateRepository;
import info.prorabka.abik.server.charge.repository.SubscriptionPollRepository;
import info.prorabka.abik.server.charge.repository.SubscriptionPollVoteRepository;
import info.prorabka.abik.server.common.exception.BadRequestException;
import info.prorabka.abik.server.common.exception.ForbiddenException;
import info.prorabka.abik.server.common.exception.NotFoundException;
import info.prorabka.abik.server.geo.entity.Region;
import info.prorabka.abik.server.geo.repository.RegionRepository;
import info.prorabka.abik.server.notification.service.NotificationService;
import info.prorabka.abik.server.team.entity.MemberStatus;
import info.prorabka.abik.server.team.entity.Team;
import info.prorabka.abik.server.team.entity.TeamMember;
import info.prorabka.abik.server.team.entity.TeamStatus;
import info.prorabka.abik.server.team.repository.TeamMemberRepository;
import info.prorabka.abik.server.team.repository.TeamRepository;
import info.prorabka.abik.server.team.service.TeamMemberService;
import info.prorabka.abik.server.user.entity.UserPosition;
import info.prorabka.abik.server.user.entity.UserProfile;
import info.prorabka.abik.server.user.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Ежемесячные опросы и начисления по шаблону «Абонемент (опрос)». */
@Service
public class SubscriptionPollService {
    private static final Logger log = LoggerFactory.getLogger(SubscriptionPollService.class);
    private static final long ROUNDING_KOPECKS = 10000L;

    private final SubscriptionPollRepository pollRepository;
    private final SubscriptionPollVoteRepository voteRepository;
    private final ChargeTemplateRepository templateRepository;
    private final ChargeRepository chargeRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository memberRepository;
    private final TeamMemberService memberService;
    private final UserProfileRepository profileRepository;
    private final RegionRepository regionRepository;
    private final NotificationService notificationService;

    public SubscriptionPollService(SubscriptionPollRepository pollRepository,
                                   SubscriptionPollVoteRepository voteRepository,
                                   ChargeTemplateRepository templateRepository,
                                   ChargeRepository chargeRepository,
                                   TeamRepository teamRepository,
                                   TeamMemberRepository memberRepository,
                                   TeamMemberService memberService,
                                   UserProfileRepository profileRepository,
                                   RegionRepository regionRepository,
                                   NotificationService notificationService) {
        this.pollRepository = pollRepository;
        this.voteRepository = voteRepository;
        this.templateRepository = templateRepository;
        this.chargeRepository = chargeRepository;
        this.teamRepository = teamRepository;
        this.memberRepository = memberRepository;
        this.memberService = memberService;
        this.profileRepository = profileRepository;
        this.regionRepository = regionRepository;
        this.notificationService = notificationService;
    }

    /** Текущий открытый опрос; при необходимости создаёт его без ожидания планировщика. */
    @Transactional
    public SubscriptionPollResponse current(Long teamId, Long userId) {
        memberService.requireMember(teamId, userId);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Команда не найдена"));
        LocalDate today = teamDate(team, LocalDate.now());
        createPollIfDue(team, today);
        return pollRepository
                .findByTeamIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByEndDateDesc(
                        teamId, today, today)
                .stream()
                .filter(poll -> isTemplateActive(poll.getTemplateId()))
                .findFirst()
                .map(poll -> toResponse(poll, userId))
                .orElse(null);
    }

    /** В период опроса участник может добавить или изменить свой ответ. */
    @Transactional
    public SubscriptionPollResponse vote(Long teamId, Long pollId, Long userId,
                                         SubscriptionPollVoteRequest request) {
        memberService.requireMember(teamId, userId);
        SubscriptionPoll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new NotFoundException("Опрос не найден"));
        if (!poll.getTeamId().equals(teamId)) {
            throw new ForbiddenException("Опрос принадлежит другой команде");
        }
        if (!isTemplateActive(poll.getTemplateId())) {
            throw new BadRequestException("Опрос завершён");
        }
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Команда не найдена"));
        LocalDate today = teamDate(team, LocalDate.now());
        if (today.isBefore(poll.getStartDate()) || today.isAfter(poll.getEndDate())) {
            throw new BadRequestException("Голосование доступно только в период опроса");
        }

        SubscriptionPollVote vote = voteRepository.findByPollIdAndUserId(pollId, userId)
                .orElseGet(() -> SubscriptionPollVote.builder().pollId(pollId).userId(userId).build());
        vote.setAnswer(request.answer());
        voteRepository.save(vote);
        return toResponse(poll, userId);
    }

    /** Ежечасная идемпотентная обработка: создание опросов и начисление после их окончания. */
    @Transactional
    public int process(LocalDate today) {
        int created = 0;
        for (Team team : teamRepository.findByStatusOrderByNameAsc(TeamStatus.ACTIVE)) {
            try {
                created += processTeam(team, teamDate(team, today));
            } catch (Exception e) {
                log.error("Ошибка обработки абонемента по опросу: teamId={}", team.getId(), e);
            }
        }
        return created;
    }

    private int processTeam(Team team, LocalDate today) {
        int created = 0;
        List<ChargeTemplate> templates = templateRepository
                .findByTeamIdAndActiveOrderByCreatedAtDesc(team.getId(), true)
                .stream()
                .filter(SubscriptionPollService::isPollTemplate)
                .toList();
        for (ChargeTemplate template : templates) {
            createPollIfDue(team, today, template);
            for (SubscriptionPoll poll : pollRepository.findByTeamIdAndProcessedAtIsNull(team.getId())) {
                if (template.getId().equals(poll.getTemplateId()) && today.isAfter(poll.getEndDate())) {
                    created += createCharges(team, poll);
                }
            }
        }
        return created;
    }

    private void createPollIfDue(Team team, LocalDate today) {
        templateRepository.findByTeamIdAndActiveOrderByCreatedAtDesc(team.getId(), true)
                .stream()
                .filter(SubscriptionPollService::isPollTemplate)
                .findFirst()
                .ifPresent(template -> createPollIfDue(team, today, template));
    }

    private void createPollIfDue(Team team, LocalDate today, ChargeTemplate template) {
        YearMonth period = YearMonth.from(today);
        LocalDate start = period.atDay(Math.min(template.getPollStartDay(), period.lengthOfMonth()));
        LocalDate end = period.atDay(Math.min(template.getPollEndDay(), period.lengthOfMonth()));
        // На следующий день ещё создаём пропущенный опрос и сразу обрабатываем его.
        if (today.isBefore(start) || today.isAfter(end.plusDays(1))) return;
        if (pollRepository.findByTemplateIdAndPeriodYearAndPeriodMonth(
                template.getId(), (short) period.getYear(), (short) period.getMonthValue()).isPresent()) return;

        YearMonth validMonth = period.plusMonths(1);
        SubscriptionPoll poll = SubscriptionPoll.builder()
                .teamId(team.getId())
                .templateId(template.getId())
                .title(template.getName())
                .groupId(template.getGroupId())
                .amountKopecks(template.getAmountKopecks())
                .distribution(template.getDistribution())
                .periodYear((short) period.getYear())
                .periodMonth((short) period.getMonthValue())
                .startDate(start)
                .endDate(end)
                .validFrom(validMonth.atDay(1))
                .validUntil(validMonth.atEndOfMonth())
                .build();
        pollRepository.save(poll);
        memberRepository.findByTeamIdAndStatusOrderByJoinedAtAsc(team.getId(), MemberStatus.ACTIVE)
                .forEach(member -> notificationService.notifySubscriptionPollCreated(
                        member.getUserId(), team.getId(), team.getName(), poll.getId(),
                        poll.getValidFrom(), poll.getValidUntil()));
        log.info("Создан опрос абонемента: teamId={}, templateId={}, период={}",
                team.getId(), template.getId(), period);
    }

    private int createCharges(Team team, SubscriptionPoll poll) {
        Set<Long> yesUserIds = voteRepository.findByPollId(poll.getId()).stream()
                .filter(vote -> vote.getAnswer() == SubscriptionPollAnswer.YES)
                .map(SubscriptionPollVote::getUserId)
                .collect(Collectors.toSet());
        List<TeamMember> recipients = memberRepository
                .findByTeamIdAndStatusOrderByJoinedAtAsc(team.getId(), MemberStatus.ACTIVE)
                .stream()
                .filter(member -> yesUserIds.contains(member.getUserId()))
                .filter(member -> !Boolean.TRUE.equals(member.getIsTrainer()))
                .filter(member -> !isGoalkeeper(member.getUserId()))
                .toList();

        int created = 0;
        if (!recipients.isEmpty()) {
            long amountPerUser = poll.getAmountKopecks();
            if (poll.getDistribution() == ChargeDistribution.EQUAL) {
                amountPerUser = ChargeService.roundUpTo100Rub(amountPerUser / recipients.size());
                if (amountPerUser <= 0) amountPerUser = ROUNDING_KOPECKS;
            }
            for (TeamMember member : recipients) {
                Short year = (short) poll.getValidFrom().getYear();
                Short month = (short) poll.getValidFrom().getMonthValue();
                if (chargeRepository.findByTemplateIdAndUserIdAndPeriodYearAndPeriodMonth(
                        poll.getTemplateId(), member.getUserId(), year, month).isPresent()) continue;
                Charge charge = chargeRepository.save(Charge.builder()
                        .teamId(team.getId())
                        .templateId(poll.getTemplateId())
                        .groupId(poll.getGroupId())
                        .userId(member.getUserId())
                        .amountKopecks(amountPerUser)
                        .paidKopecks(0L)
                        .status(ChargeStatus.ACTIVE)
                        .description(poll.getTitle())
                        .dueDate(poll.getValidFrom())
                        .periodYear(year)
                        .periodMonth(month)
                        .build());
                notificationService.notifyNewCharge(member.getUserId(), team.getId(), team.getName(),
                        charge.getId(), charge.getAmountKopecks(), charge.getDescription());
                created++;
            }
        }
        poll.setProcessedAt(OffsetDateTime.now());
        pollRepository.save(poll);
        if (created > 0) {
            log.info("Начисления по опросу созданы: pollId={}, количество={}", poll.getId(), created);
        }
        return created;
    }

    /** Действующие абонементы пользователя по всем активным командам. */
    @Transactional(readOnly = true)
    public List<ActiveSubscriptionResponse> activeSubscriptions(Long userId) {
        return memberRepository.findByUserIdAndStatusOrderByJoinedAtDesc(userId, MemberStatus.ACTIVE)
                .stream()
                .map(member -> teamRepository.findById(member.getTeamId()).orElse(null))
                .filter(team -> team != null && team.getStatus() == TeamStatus.ACTIVE)
                .flatMap(team -> {
                    YearMonth validMonth = YearMonth.from(teamDate(team, LocalDate.now()));
                    return chargeRepository.findSubscriptionCharges(
                                    team.getId(), userId,
                                    (short) validMonth.getYear(), (short) validMonth.getMonthValue(),
                                    List.of(ChargeStatus.ACTIVE, ChargeStatus.PARTIALLY_PAID, ChargeStatus.PAID))
                            .stream()
                            .map(charge -> new ActiveSubscriptionResponse(
                                    team.getId(), team.getName(),
                                    charge.getDescription() == null || charge.getDescription().isBlank()
                                            ? "Абонемент" : charge.getDescription(),
                                    validMonth.atDay(1), validMonth.atEndOfMonth()));
                })
                .sorted(Comparator.comparing(ActiveSubscriptionResponse::teamName)
                        .thenComparing(ActiveSubscriptionResponse::validFrom))
                .toList();
    }

    private SubscriptionPollResponse toResponse(SubscriptionPoll poll, Long currentUserId) {
        List<SubscriptionPollVote> votes = voteRepository.findByPollId(poll.getId());
        int yes = (int) votes.stream().filter(v -> v.getAnswer() == SubscriptionPollAnswer.YES).count();
        int no = votes.size() - yes;
        SubscriptionPollAnswer myVote = votes.stream()
                .filter(v -> v.getUserId().equals(currentUserId))
                .map(SubscriptionPollVote::getAnswer)
                .findFirst().orElse(null);
        List<SubscriptionPollResponse.SubscriptionPollVoterResponse> positive = votes.stream()
                .filter(v -> v.getAnswer() == SubscriptionPollAnswer.YES)
                .map(v -> profileRepository.findById(v.getUserId())
                        .map(profile -> new SubscriptionPollResponse.SubscriptionPollVoterResponse(
                                v.getUserId(), shortName(profile)))
                        .orElse(new SubscriptionPollResponse.SubscriptionPollVoterResponse(
                                v.getUserId(), "Участник")))
                .sorted(Comparator.comparing(SubscriptionPollResponse.SubscriptionPollVoterResponse::name))
                .toList();
        return new SubscriptionPollResponse(poll.getId(), poll.getTemplateId(), poll.getTitle(),
                poll.getStartDate(), poll.getEndDate(), poll.getValidFrom(), poll.getValidUntil(),
                yes, no, myVote, positive);
    }

    private boolean isTemplateActive(Long templateId) {
        return templateRepository.findById(templateId)
                .filter(SubscriptionPollService::isPollTemplate)
                .map(ChargeTemplate::getActive)
                .filter(Boolean.TRUE::equals)
                .isPresent();
    }

    private static boolean isPollTemplate(ChargeTemplate template) {
        return Boolean.TRUE.equals(template.getPollEnabled());
    }

    private boolean isGoalkeeper(Long userId) {
        return profileRepository.findById(userId)
                .map(profile -> profile.getPositions().contains(UserPosition.GOALKEEPER))
                .orElse(false);
    }

    private LocalDate teamDate(Team team, LocalDate fallback) {
        if (team.getHomeRegionId() == null) return fallback;
        Region region = regionRepository.findById(team.getHomeRegionId()).orElse(null);
        if (region == null || region.getTimezone() == null) return fallback;
        return ZonedDateTime.now(ZoneId.of(region.getTimezone())).toLocalDate();
    }

    private static String shortName(UserProfile profile) {
        String firstInitial = profile.getFirstName() == null || profile.getFirstName().isBlank()
                ? "" : " " + profile.getFirstName().trim().substring(0, 1) + ".";
        return (profile.getLastName() == null ? "" : profile.getLastName().trim()) + firstInitial;
    }
}
