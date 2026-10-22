package info.prorabka.abik.server.event.service;

import info.prorabka.abik.server.common.exception.BadRequestException;
import info.prorabka.abik.server.common.exception.ForbiddenException;
import info.prorabka.abik.server.common.exception.NotFoundException;
import info.prorabka.abik.server.event.dto.*;
import info.prorabka.abik.server.event.entity.*;
import info.prorabka.abik.server.event.repository.*;
import info.prorabka.abik.server.notification.service.NotificationService;
import info.prorabka.abik.server.team.entity.MemberStatus;
import info.prorabka.abik.server.team.entity.TeamMember;
import info.prorabka.abik.server.team.repository.TeamMemberRepository;
import info.prorabka.abik.server.team.service.TeamMemberService;
import info.prorabka.abik.server.user.entity.UserPosition;
import info.prorabka.abik.server.user.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

@Service
public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    private final EventRepository eventRepo;
    private final EventSeriesRepository seriesRepo;
    private final EventVoteRepository voteRepo;
    private final EventSweaterRepository sweaterRepo;
    private final EventLineupRepository lineupRepo;
    private final TeamMemberService teamMemberService;
    private final TeamMemberRepository teamMemberRepo;
    private final UserProfileRepository profileRepo;
    private final NotificationService notificationService;

    public EventService(EventRepository eventRepo,
                        EventSeriesRepository seriesRepo,
                        EventVoteRepository voteRepo,
                        EventSweaterRepository sweaterRepo,
                        EventLineupRepository lineupRepo,
                        TeamMemberService teamMemberService,
                        TeamMemberRepository teamMemberRepo,
                        UserProfileRepository profileRepo,
                        NotificationService notificationService) {
        this.eventRepo = eventRepo;
        this.seriesRepo = seriesRepo;
        this.voteRepo = voteRepo;
        this.sweaterRepo = sweaterRepo;
        this.lineupRepo = lineupRepo;
        this.teamMemberService = teamMemberService;
        this.teamMemberRepo = teamMemberRepo;
        this.profileRepo = profileRepo;
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
            result.add(new EventListItemResponse(
                    e.getId(), e.getType(), e.getEventDate(), e.getTimeOfDay(),
                    e.getPlace(), myVote, yes, no));
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

        // Звенья
        List<EventLineup> lineupRows = lineupRepo.findByEventId(eventId);
        Long gkId = lineupRows.stream()
                .filter(l -> l.getRole() == LineupRole.GOALKEEPER)
                .map(EventLineup::getUserId).findFirst().orElse(null);

        Map<Short, List<Long>> defs = new TreeMap<>();
        Map<Short, List<Long>> fwds = new TreeMap<>();
        for (EventLineup l : lineupRows) {
            if (l.getRole() == LineupRole.DEFENDER) {
                defs.computeIfAbsent(l.getLineNumber(), k -> new ArrayList<>()).add(l.getUserId());
            } else if (l.getRole() == LineupRole.FORWARD) {
                fwds.computeIfAbsent(l.getLineNumber(), k -> new ArrayList<>()).add(l.getUserId());
            }
        }
        Set<Short> lineNums = new TreeSet<>();
        lineNums.addAll(defs.keySet());
        lineNums.addAll(fwds.keySet());
        List<LineDto> lines = new ArrayList<>();
        for (Short n : lineNums) {
            lines.add(new LineDto(n.intValue(),
                    defs.getOrDefault(n, List.of()),
                    fwds.getOrDefault(n, List.of())));
        }
        LineupDto lineup = lineupRows.isEmpty() ? null : new LineupDto(gkId, lines);

        return new EventDetailResponse(
                e.getId(), e.getType(), e.getEventDate(), e.getTimeOfDay(),
                e.getPlace(), e.getNote(), e.getStatus(),
                myVote, voters, lineup);
    }

    // ====== Создание ======

    @Transactional
    public EventDetailResponse create(Long teamId, Long actor, EventCreateRequest req) {
        teamMemberService.requireManager(teamId, actor);

        if (req.recurrence() == null) {
            Event e = eventRepo.save(Event.builder()
                    .teamId(teamId).type(req.type())
                    .eventDate(req.date()).timeOfDay(req.time())
                    .place(req.place().trim()).note(trim(req.note()))
                    .status("ACTIVE").createdBy(actor).build());
            return getDetail(teamId, e.getId(), actor);
        }

        // Серия
        Short[] weekdays = req.recurrence().weekdays().toArray(new Short[0]);
        EventSeries series = seriesRepo.save(EventSeries.builder()
                .teamId(teamId).type(req.type())
                .timeOfDay(req.time()).place(req.place().trim())
                .note(trim(req.note())).weekdays(weekdays)
                .weeksAhead(req.recurrence().weeksAhead())
                .createdBy(actor).build());

        // Генерируем события
        LocalDate start = req.date();
        LocalDate end = start.plusWeeks(req.recurrence().weeksAhead());
        Set<Short> targetDays = req.recurrence().weekdays();
        LocalDate cur = start;
        Event first = null;
        while (!cur.isAfter(end)) {
            DayOfWeek dow = cur.getDayOfWeek();
            short isoDow = (short) (dow == DayOfWeek.SUNDAY ? 7 : dow.getValue());
            if (targetDays.contains(isoDow)) {
                Event saved = eventRepo.save(Event.builder()
                        .teamId(teamId).seriesId(series.getId())
                        .type(req.type()).eventDate(cur)
                        .timeOfDay(req.time()).place(req.place().trim())
                        .note(trim(req.note()))
                        .status("ACTIVE").createdBy(actor).build());
                if (first == null) first = saved;
            }
            cur = cur.plusDays(1);
        }
        if (first == null) {
            throw new BadRequestException("Дата не совпадает ни с одним днём недели серии");
        }
        return getDetail(teamId, first.getId(), actor);
    }

    // ====== Обновление/удаление ======

    @Transactional
    public EventDetailResponse update(Long teamId, Long eventId, Long actor,
                                      EventUpdateRequest req, String scope) {
        teamMemberService.requireManager(teamId, actor);
        Event e = getEventOrThrow(eventId);
        if (!e.getTeamId().equals(teamId)) throw new ForbiddenException("Событие другой команды");

        e.setType(req.type());
        e.setEventDate(req.date());
        e.setTimeOfDay(req.time());
        e.setPlace(req.place().trim());
        e.setNote(trim(req.note()));
        eventRepo.save(e);

        if ("series".equals(scope) && e.getSeriesId() != null) {
            // Обновить и все будущие события серии
            List<Event> future = eventRepo.findBySeriesIdAndEventDateGreaterThanEqual(
                    e.getSeriesId(), req.date());
            for (Event f : future) {
                if (f.getId().equals(eventId)) continue;
                f.setType(req.type());
                f.setTimeOfDay(req.time());
                f.setPlace(req.place().trim());
                f.setNote(trim(req.note()));
                eventRepo.save(f);
            }
        }
        return getDetail(teamId, eventId, actor);
    }

    @Transactional
    public void delete(Long teamId, Long eventId, Long actor, String scope) {
        teamMemberService.requireManager(teamId, actor);
        Event e = getEventOrThrow(eventId);
        if (!e.getTeamId().equals(teamId)) throw new ForbiddenException("Событие другой команды");

        if ("series".equals(scope) && e.getSeriesId() != null) {
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
        boolean canManage = actorMember.getRole() == info.prorabka.abik.server.team.entity.MemberRole.OWNER
                || actorMember.getRole() == info.prorabka.abik.server.team.entity.MemberRole.ADMIN
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

    // ====== Lineup ======

    @Transactional
    public void setLineup(Long teamId, Long eventId, Long actor, LineupSaveRequest req) {
        teamMemberService.requireManager(teamId, actor);
        Event e = getEventOrThrow(eventId);
        if (!e.getTeamId().equals(teamId)) throw new ForbiddenException("Событие другой команды");
        if (e.getType() == EventType.TRAINING) {
            throw new BadRequestException("Звенья доступны только для игр и матчей");
        }

        // Проверить, что все — YES и не тренеры
        Set<Long> allUsers = new HashSet<>();
        if (req.goalkeeperId() != null) allUsers.add(req.goalkeeperId());
        for (var line : req.lines()) {
            allUsers.addAll(line.defenderIds());
            allUsers.addAll(line.forwardIds());
        }
        for (Long uid : allUsers) {
            EventVote v = voteRepo.findByEventIdAndUserId(eventId, uid).orElse(null);
            if (v == null || v.getAnswer() != VoteAnswer.YES) {
                throw new BadRequestException(
                        "В звенья можно включать только проголосовавших YES: userId=" + uid);
            }
            Boolean isTrainer = teamMemberRepo.findByTeamIdAndUserId(teamId, uid)
                    .map(TeamMember::getIsTrainer).orElse(false);
            if (Boolean.TRUE.equals(isTrainer)) {
                throw new BadRequestException("Тренеров нельзя включать в звенья: userId=" + uid);
            }
        }

        lineupRepo.deleteByEventId(eventId);

        if (req.goalkeeperId() != null) {
            lineupRepo.save(EventLineup.builder()
                    .eventId(eventId).userId(req.goalkeeperId())
                    .role(LineupRole.GOALKEEPER).lineNumber(null).build());
        }

        short lineNum = 1;
        for (var line : req.lines()) {
            for (Long d : line.defenderIds()) {
                lineupRepo.save(EventLineup.builder()
                        .eventId(eventId).userId(d)
                        .role(LineupRole.DEFENDER).lineNumber(lineNum).build());
            }
            for (Long f : line.forwardIds()) {
                lineupRepo.save(EventLineup.builder()
                        .eventId(eventId).userId(f)
                        .role(LineupRole.FORWARD).lineNumber(lineNum).build());
            }
            lineNum++;
        }
    }

    // ====== Helpers ======

    private Event getEventOrThrow(Long id) {
        return eventRepo.findById(id).orElseThrow(() -> new NotFoundException("Событие не найдено"));
    }

    private String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}