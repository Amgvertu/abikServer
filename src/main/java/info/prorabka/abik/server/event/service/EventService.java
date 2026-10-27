package info.prorabka.abik.server.event.service;

import info.prorabka.abik.server.common.exception.BadRequestException;
import info.prorabka.abik.server.common.exception.ForbiddenException;
import info.prorabka.abik.server.common.exception.NotFoundException;
import info.prorabka.abik.server.event.dto.*;
import info.prorabka.abik.server.event.entity.*;
import info.prorabka.abik.server.event.repository.*;
import info.prorabka.abik.server.notification.service.NotificationService;
import info.prorabka.abik.server.roster.entity.ChampionshipRoster;
import info.prorabka.abik.server.roster.repository.ChampionshipRosterRepository;
import info.prorabka.abik.server.team.entity.MemberRole;
import info.prorabka.abik.server.team.entity.MemberStatus;
import info.prorabka.abik.server.team.entity.Team;
import info.prorabka.abik.server.team.entity.TeamMember;
import info.prorabka.abik.server.team.repository.TeamMemberRepository;
import info.prorabka.abik.server.team.repository.TeamRepository;
import info.prorabka.abik.server.team.service.TeamMemberService;
import info.prorabka.abik.server.user.entity.UserPosition;
import info.prorabka.abik.server.user.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final EventRepository eventRepo;
    private final EventSeriesRepository seriesRepo;
    private final EventVoteRepository voteRepo;
    private final EventSweaterRepository sweaterRepo;
    private final EventLineupRepository lineupRepo;
    private final TeamMemberService teamMemberService;
    private final TeamMemberRepository teamMemberRepo;
    private final TeamRepository teamRepo;
    private final UserProfileRepository profileRepo;
    private final ChampionshipRosterRepository rosterRepo;
    private final NotificationService notificationService;

    public EventService(EventRepository eventRepo,
                        EventSeriesRepository seriesRepo,
                        EventVoteRepository voteRepo,
                        EventSweaterRepository sweaterRepo,
                        EventLineupRepository lineupRepo,
                        TeamMemberService teamMemberService,
                        TeamMemberRepository teamMemberRepo,
                        TeamRepository teamRepo,
                        UserProfileRepository profileRepo,
                        ChampionshipRosterRepository rosterRepo,
                        NotificationService notificationService) {
        this.eventRepo = eventRepo;
        this.seriesRepo = seriesRepo;
        this.voteRepo = voteRepo;
        this.sweaterRepo = sweaterRepo;
        this.lineupRepo = lineupRepo;
        this.teamMemberService = teamMemberService;
        this.teamMemberRepo = teamMemberRepo;
        this.teamRepo = teamRepo;
        this.profileRepo = profileRepo;
        this.rosterRepo = rosterRepo;
        this.notificationService = notificationService;
    }

    // ====== Чтение ======

    public List<EventListItemResponse> list(Long teamId, Long actor, LocalDate from, LocalDate to) {
        teamMemberService.requireMember(teamId, actor);

        List<Event> events = eventRepo
                .findByTeamIdAndEventDateBetweenOrderByEventDateAscTimeOfDayAsc(teamId, from, to);

        List<EventListItemResponse> result = new ArrayList<>();
        for (Event e : events) {
            VoteAnswer myVote = voteRepo.findByEventIdAndUserId(e.getId(), actor)
                    .map(EventVote::getAnswer).orElse(null);
            long yes = voteRepo.countByEventIdAndAnswer(e.getId(), VoteAnswer.YES);
            long no  = voteRepo.countByEventIdAndAnswer(e.getId(), VoteAnswer.NO);
            long maybe = voteRepo.countByEventIdAndAnswer(e.getId(), VoteAnswer.MAYBE);
            result.add(new EventListItemResponse(
                    e.getId(), e.getTitle(), e.getSection(), e.getType(),
                    e.getEventDate(), e.getTimeOfDay(),
                    e.getPlace(), myVote, yes, no, maybe));
        }
        return result;
    }

    public EventDetailResponse getDetail(Long teamId, Long eventId, Long actor) {
        teamMemberService.requireMember(teamId, actor);
        Event e = getEventOrThrow(eventId);
        if (!e.getTeamId().equals(teamId)) throw new ForbiddenException("Событие другой команды");

        // Голоса
        List<EventVote> votes = voteRepo.findByEventId(eventId);
        Map<Long, SweaterColor> colors = new HashMap<>();
        for (EventSweater s : sweaterRepo.findByEventId(eventId)) {
            colors.put(s.getUserId(), s.getColor());
        }

        List<EventVoterDto> voters = new ArrayList<>();
        for (EventVote v : votes) {
            String fullName = profileRepo.findById(v.getUserId())
                    .map(p -> p.getLastName() + " " + p.getFirstName())
                    .orElse("user#" + v.getUserId());
            Set<UserPosition> positions = profileRepo.findById(v.getUserId())
                    .map(p -> (Set<UserPosition>) new HashSet<>(p.getPositions()))
                    .orElse(Set.of());
            Boolean isTrainer = teamMemberRepo.findByTeamIdAndUserId(teamId, v.getUserId())
                    .map(TeamMember::getIsTrainer).orElse(false);
            voters.add(new EventVoterDto(
                    v.getUserId(), fullName, v.getAnswer(), positions,
                    isTrainer, colors.get(v.getUserId())));
        }

        VoteAnswer myVote = votes.stream()
                .filter(v -> v.getUserId().equals(actor))
                .map(EventVote::getAnswer).findFirst().orElse(null);

        // Заявка (lineup): стабильный порядок — вратарь, затем звенья
        List<EventLineup> lineupRows = lineupRepo.findByEventId(eventId);
        LineupDto lineup = null;
        if (!lineupRows.isEmpty()) {
            List<LineupSlotDto> slots = new ArrayList<>();
            lineupRows.stream()
                    .filter(l -> l.getRole() == LineupRole.GOALKEEPER)
                    .sorted(Comparator.comparing(EventLineup::getSlotNumber))
                    .forEach(l -> slots.add(toSlot(l)));
            lineupRows.stream()
                    .filter(l -> l.getRole() != LineupRole.GOALKEEPER)
                    .sorted(Comparator.comparing(EventLineup::getLineNumber)
                            .thenComparing(l -> l.getRole() == LineupRole.DEFENDER ? 0 : 1)
                            .thenComparing(EventLineup::getSlotNumber))
                    .forEach(l -> slots.add(toSlot(l)));
            lineup = new LineupDto(slots);
        }

        return new EventDetailResponse(
                e.getId(), e.getTitle(), e.getSection(), e.getType(),
                e.getEventDate(), e.getTimeOfDay(),
                e.getPlace(), e.getNote(), e.getStatus(),
                e.getSeriesId(),
                myVote, voters, lineup);
    }

    private LineupSlotDto toSlot(EventLineup l) {
        return new LineupSlotDto(
                l.getLineNumber() == null ? null : l.getLineNumber().intValue(),
                l.getSlotNumber() == null ? null : l.getSlotNumber().intValue(),
                l.getRole(), l.getUserId());
    }

    // ====== Создание ======

    @Transactional
    public EventDetailResponse create(Long teamId, Long actor, EventCreateRequest req) {
        teamMemberService.requireManager(teamId, actor);
        validateSectionType(req.section(), req.type());

        if (req.recurrence() == null) {
            Event e = eventRepo.save(Event.builder()
                    .teamId(teamId).title(req.title().trim())
                    .section(req.section()).type(req.type())
                    .eventDate(req.date()).timeOfDay(req.time())
                    .place(req.place().trim()).note(trim(req.note()))
                    .status("ACTIVE").createdBy(actor).build());
            notifyCreated(teamId, actor, e, false);
            return getDetail(teamId, e.getId(), actor);
        }

        // Серия
        LocalDate start = req.date();
        LocalDate end = req.recurrence().untilDate();
        if (end.isBefore(start)) {
            throw new BadRequestException("Дата окончания серии раньше даты начала");
        }
        if (end.isAfter(start.plusYears(2))) {
            throw new BadRequestException("Серия не может быть длиннее двух лет");
        }

        Short[] weekdays = req.recurrence().weekdays().toArray(new Short[0]);
        EventSeries series = seriesRepo.save(EventSeries.builder()
                .teamId(teamId).title(req.title().trim())
                .section(req.section()).type(req.type())
                .timeOfDay(req.time()).place(req.place().trim())
                .note(trim(req.note())).weekdays(weekdays)
                .untilDate(end)
                .createdBy(actor).build());

        Set<Short> targetDays = req.recurrence().weekdays();
        LocalDate cur = start;
        Event first = null;
        while (!cur.isAfter(end)) {
            DayOfWeek dow = cur.getDayOfWeek();
            short isoDow = (short) (dow == DayOfWeek.SUNDAY ? 7 : dow.getValue());
            if (targetDays.contains(isoDow)) {
                Event saved = eventRepo.save(Event.builder()
                        .teamId(teamId).seriesId(series.getId())
                        .title(req.title().trim())
                        .section(req.section()).type(req.type())
                        .eventDate(cur)
                        .timeOfDay(req.time()).place(req.place().trim())
                        .note(trim(req.note()))
                        .status("ACTIVE").createdBy(actor).build());
                if (first == null) first = saved;
            }
            cur = cur.plusDays(1);
        }
        if (first == null) {
            throw new BadRequestException("В период серии не попадает ни один выбранный день недели");
        }
        notifyCreated(teamId, actor, first, true);
        return getDetail(teamId, first.getId(), actor);
    }

    // ====== Обновление/удаление ======

    @Transactional
    public EventDetailResponse update(Long teamId, Long eventId, Long actor,
                                      EventUpdateRequest req, String scope) {
        teamMemberService.requireManager(teamId, actor);
        Event e = getEventOrThrow(eventId);
        if (!e.getTeamId().equals(teamId)) throw new ForbiddenException("Событие другой команды");
        validateSectionType(req.section(), req.type());

        boolean changed = significantChanged(e, req);

        e.setTitle(req.title().trim());
        e.setSection(req.section());
        e.setType(req.type());
        e.setEventDate(req.date());
        e.setTimeOfDay(req.time());
        e.setPlace(req.place().trim());
        e.setNote(trim(req.note()));
        eventRepo.save(e);

        if ("series".equals(scope) && e.getSeriesId() != null) {
            // Обновить шаблон серии
            seriesRepo.findById(e.getSeriesId()).ifPresent(s -> {
                s.setTitle(req.title().trim());
                s.setSection(req.section());
                s.setType(req.type());
                s.setTimeOfDay(req.time());
                s.setPlace(req.place().trim());
                s.setNote(trim(req.note()));
                seriesRepo.save(s);
            });
            // Обновить и все будущие события серии
            List<Event> future = eventRepo.findBySeriesIdAndEventDateGreaterThanEqual(
                    e.getSeriesId(), req.date());
            for (Event f : future) {
                if (f.getId().equals(eventId)) continue;
                f.setTitle(req.title().trim());
                f.setSection(req.section());
                f.setType(req.type());
                f.setTimeOfDay(req.time());
                f.setPlace(req.place().trim());
                f.setNote(trim(req.note()));
                eventRepo.save(f);
            }
        }

        if (changed) {
            notifyUpdated(teamId, actor, e);
        }
        return getDetail(teamId, eventId, actor);
    }

    private boolean significantChanged(Event e, EventUpdateRequest req) {
        return !e.getTitle().equals(req.title().trim())
                || e.getSection() != req.section()
                || e.getType() != req.type()
                || !e.getEventDate().equals(req.date())
                || !e.getTimeOfDay().equals(req.time())
                || !e.getPlace().equals(req.place().trim())
                || !Objects.equals(e.getNote(), trim(req.note()));
    }

    @Transactional
    public void delete(Long teamId, Long eventId, Long actor, String scope) {
        teamMemberService.requireManager(teamId, actor);
        Event e = getEventOrThrow(eventId);
        if (!e.getTeamId().equals(teamId)) throw new ForbiddenException("Событие другой команды");

        boolean withSeries = "series".equals(scope) && e.getSeriesId() != null;
        notifyCancelled(teamId, actor, e, withSeries);

        if (withSeries) {
            List<Event> future = eventRepo.findBySeriesIdAndEventDateGreaterThanEqual(
                    e.getSeriesId(), e.getEventDate());
            eventRepo.deleteAll(future);
        } else {
            eventRepo.delete(e);
        }
    }

    // ====== Голос ======

    @Transactional
    public void vote(Long teamId, Long eventId, Long actor, VoteAnswer answer) {
        teamMemberService.requireMember(teamId, actor);
        Event e = getEventOrThrow(eventId);
        if (!e.getTeamId().equals(teamId)) throw new ForbiddenException("Событие другой команды");
        if (!"ACTIVE".equals(e.getStatus())) {
            throw new BadRequestException("Событие отменено");
        }

        EventVote vote = voteRepo.findByEventIdAndUserId(eventId, actor)
                .orElseGet(() -> EventVote.builder()
                        .eventId(eventId).userId(actor).build());
        vote.setAnswer(answer);
        voteRepo.save(vote);
    }

    // ====== Свитера ======

    @Transactional
    public void setSweaterColors(Long teamId, Long eventId, Long actor,
                                 List<SweaterColorItemDto> items) {
        TeamMember actorMember = teamMemberService.requireMember(teamId, actor);
        Event e = getEventOrThrow(eventId);
        if (!e.getTeamId().equals(teamId)) throw new ForbiddenException("Событие другой команды");
        if (e.getType() != EventType.TRAINING) {
            throw new BadRequestException("Цвета свитеров — только для тренировок");
        }

        // Права: OWNER, ADMIN, тренер
        boolean isTrainer = Boolean.TRUE.equals(actorMember.getIsTrainer());
        boolean canManage = actorMember.getRole() == MemberRole.OWNER
                || actorMember.getRole() == MemberRole.ADMIN
                || isTrainer;
        if (!canManage) {
            throw new ForbiddenException("Назначать цвета может владелец, админ или тренер");
        }

        // Только проголосовавшим YES
        for (SweaterColorItemDto item : items) {
            EventVote v = voteRepo.findByEventIdAndUserId(eventId, item.userId()).orElse(null);
            if (item.color() != null && (v == null || v.getAnswer() != VoteAnswer.YES)) {
                throw new BadRequestException(
                        "Цвет можно назначить только проголосовавшим YES: userId=" + item.userId());
            }
        }

        sweaterRepo.deleteByEventId(eventId);
        for (SweaterColorItemDto item : items) {
            if (item.color() == null) continue;
            sweaterRepo.save(EventSweater.builder()
                    .eventId(eventId).userId(item.userId()).color(item.color()).build());
        }
    }

    // ====== Заявка на игру (lineup) ======

    @Transactional
    public void setLineup(Long teamId, Long eventId, Long actor, LineupSaveRequest req) {
        TeamMember actorMember = teamMemberService.requireMember(teamId, actor);
        if (actorMember.getRole() != MemberRole.OWNER && actorMember.getRole() != MemberRole.ADMIN) {
            throw new ForbiddenException("Заявку на игру может менять только владелец или админ");
        }
        Event e = getEventOrThrow(eventId);
        if (!e.getTeamId().equals(teamId)) throw new ForbiddenException("Событие другой команды");
        if (e.getType() == EventType.TRAINING) {
            throw new BadRequestException("Заявка доступна только для игр и матчей");
        }

        List<LineupSlotDto> slots = req.slots() == null ? List.of() : req.slots();

        // --- Структурные проверки ---
        Set<Long> seenUsers = new HashSet<>();
        Set<String> seenSlots = new HashSet<>();
        Map<Integer, int[]> perLine = new HashMap<>();  // lineNumber -> [защитники, нападающие]
        int gkCount = 0;
        for (LineupSlotDto s : slots) {
            if (s.slotNumber() == null || s.role() == null || s.userId() == null) {
                throw new BadRequestException("В каждом слоте должны быть slotNumber, role и userId");
            }
            if (!seenUsers.add(s.userId())) {
                throw new BadRequestException(
                        "Один игрок не может занимать два слота: userId=" + s.userId());
            }
            switch (s.role()) {
                case GOALKEEPER -> {
                    gkCount++;
                    if (gkCount > 1) {
                        throw new BadRequestException("В заявке может быть только один вратарь");
                    }
                    if (s.lineNumber() != null) {
                        throw new BadRequestException("У вратаря не должно быть номера звена");
                    }
                    if (s.slotNumber() != 1) {
                        throw new BadRequestException("У вратаря номер слота должен быть 1");
                    }
                }
                case DEFENDER -> {
                    requireLineNumber(s);
                    if (s.slotNumber() < 1 || s.slotNumber() > 2) {
                        throw new BadRequestException("У защитника номер слота 1 или 2");
                    }
                    int[] c = perLine.computeIfAbsent(s.lineNumber(), k -> new int[2]);
                    c[0]++;
                    if (c[0] > 2) {
                        throw new BadRequestException(
                                "В звене " + s.lineNumber() + " больше двух защитников");
                    }
                }
                case FORWARD -> {
                    requireLineNumber(s);
                    if (s.slotNumber() < 1 || s.slotNumber() > 3) {
                        throw new BadRequestException("У нападающего номер слота от 1 до 3");
                    }
                    int[] c = perLine.computeIfAbsent(s.lineNumber(), k -> new int[2]);
                    c[1]++;
                    if (c[1] > 3) {
                        throw new BadRequestException(
                                "В звене " + s.lineNumber() + " больше трёх нападающих");
                    }
                }
            }
            String key = s.role() + ":" + s.lineNumber() + ":" + s.slotNumber();
            if (!seenSlots.add(key)) {
                throw new BadRequestException("Слот заполнен дважды: " + key);
            }
        }

        // --- Проверки игроков ---
        Team team = teamRepo.findById(teamId)
                .orElseThrow(() -> new NotFoundException("Команда не найдена"));
        for (LineupSlotDto s : slots) {
            Long uid = s.userId();

            TeamMember member = teamMemberRepo
                    .findByTeamIdAndUserIdAndStatus(teamId, uid, MemberStatus.ACTIVE)
                    .orElseThrow(() -> new BadRequestException(
                            "Игрок не является активным участником команды: userId=" + uid));
            if (Boolean.TRUE.equals(member.getIsTrainer())) {
                throw new BadRequestException("Тренеров нельзя включать в заявку: userId=" + uid);
            }

            EventVote v = voteRepo.findByEventIdAndUserId(eventId, uid).orElse(null);
            if (v == null || v.getAnswer() != VoteAnswer.YES) {
                throw new BadRequestException(
                        "В заявку можно включать только проголосовавших «Иду»: userId=" + uid);
            }

            // Допуск к составу регулярки (текущий сезон команды)
            if (team.getSeason() != null && !team.getSeason().isBlank()) {
                ChampionshipRoster rosterEntry = rosterRepo
                        .findByTeamIdAndSeasonAndUserId(teamId, team.getSeason(), uid)
                        .orElse(null);
                if (rosterEntry == null || !"ACTIVE".equals(rosterEntry.getStatus())) {
                    throw new BadRequestException(
                            "Игрок не входит в состав регулярки сезона " + team.getSeason()
                                    + ": userId=" + uid);
                }
            }

            // Амплуа должно соответствовать роли слота
            UserPosition needed = switch (s.role()) {
                case GOALKEEPER -> UserPosition.GOALKEEPER;
                case DEFENDER -> UserPosition.DEFENDER;
                case FORWARD -> UserPosition.FORWARD;
            };
            Set<UserPosition> positions = profileRepo.findById(uid)
                    .map(p -> (Set<UserPosition>) new HashSet<>(p.getPositions()))
                    .orElse(Set.of());
            if (!positions.contains(needed)) {
                throw new BadRequestException(
                        "Амплуа игрока не соответствует слоту " + s.role() + ": userId=" + uid);
            }
        }

        // --- Сохранение ---
        lineupRepo.deleteByEventId(eventId);
        for (LineupSlotDto s : slots) {
            lineupRepo.save(EventLineup.builder()
                    .eventId(eventId).userId(s.userId()).role(s.role())
                    .lineNumber(s.lineNumber() == null ? null : s.lineNumber().shortValue())
                    .slotNumber(s.slotNumber().shortValue())
                    .build());
        }
    }

    private void requireLineNumber(LineupSlotDto s) {
        if (s.lineNumber() == null || s.lineNumber() < 1) {
            throw new BadRequestException("У полевого игрока номер звена должен быть 1 или больше");
        }
    }

    // ====== Уведомления ======

    private void notifyCreated(Long teamId, Long actor, Event e, boolean series) {
        String when = formatWhen(e) + (series ? " (серия)" : "");
        for (TeamMember m : activeMembers(teamId, actor)) {
            notificationService.notifyEventCreated(
                    m.getUserId(), teamId, teamName(teamId), e.getId(), e.getTitle(), when);
        }
    }

    private void notifyUpdated(Long teamId, Long actor, Event e) {
        String when = formatWhen(e);
        for (TeamMember m : activeMembers(teamId, actor)) {
            notificationService.notifyEventUpdated(
                    m.getUserId(), teamId, teamName(teamId), e.getId(), e.getTitle(), when);
        }
    }

    private void notifyCancelled(Long teamId, Long actor, Event e, boolean series) {
        String when = formatWhen(e) + (series ? " (серия)" : "");
        for (TeamMember m : activeMembers(teamId, actor)) {
            notificationService.notifyEventCancelled(
                    m.getUserId(), teamId, teamName(teamId), e.getId(), e.getTitle(), when);
        }
    }

    private List<TeamMember> activeMembers(Long teamId, Long exceptUserId) {
        List<TeamMember> all = teamMemberRepo
                .findByTeamIdAndStatusOrderByJoinedAtAsc(teamId, MemberStatus.ACTIVE);
        List<TeamMember> result = new ArrayList<>();
        for (TeamMember m : all) {
            if (!m.getUserId().equals(exceptUserId)) result.add(m);
        }
        return result;
    }

    private String teamName(Long teamId) {
        return teamRepo.findById(teamId).map(Team::getName).orElse("команда");
    }

    private String formatWhen(Event e) {
        return e.getEventDate().format(DATE_FMT) + " " + e.getTimeOfDay();
    }

    // ====== Helpers ======

    private void validateSectionType(EventSection section, EventType type) {
        if (section == EventSection.TRAINING && type != EventType.TRAINING) {
            throw new BadRequestException("Раздел «Тренировки» допускает только тип TRAINING");
        }
        if (section == EventSection.REGULAR && type == EventType.TRAINING) {
            throw new BadRequestException("Раздел «Регулярка» допускает типы GAME и MATCH");
        }
    }

    private Event getEventOrThrow(Long id) {
        return eventRepo.findById(id).orElseThrow(() -> new NotFoundException("Событие не найдено"));
    }

    private String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}