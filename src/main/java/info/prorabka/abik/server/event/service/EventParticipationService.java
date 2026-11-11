package info.prorabka.abik.server.event.service;

import info.prorabka.abik.server.charge.entity.ChargeList;
import info.prorabka.abik.server.charge.repository.ChargeListRepository;
import info.prorabka.abik.server.event.entity.Event;
import info.prorabka.abik.server.event.entity.EventVote;
import info.prorabka.abik.server.event.entity.VoteAnswer;
import info.prorabka.abik.server.event.repository.EventLineupRepository;
import info.prorabka.abik.server.event.repository.EventRepository;
import info.prorabka.abik.server.event.repository.EventSweaterRepository;
import info.prorabka.abik.server.event.repository.EventVoteRepository;
import info.prorabka.abik.server.notification.service.NotificationService;
import info.prorabka.abik.server.roster.entity.ChampionshipRoster;
import info.prorabka.abik.server.roster.repository.ChampionshipRosterRepository;
import info.prorabka.abik.server.team.entity.MemberStatus;
import info.prorabka.abik.server.team.entity.Team;
import info.prorabka.abik.server.team.repository.TeamMemberRepository;
import info.prorabka.abik.server.team.repository.TeamRepository;
import info.prorabka.abik.server.user.entity.UserPosition;
import info.prorabka.abik.server.user.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Очистка участия игрока только в активных будущих событиях. */
@Service
public class EventParticipationService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final EventRepository eventRepository;
    private final EventVoteRepository voteRepository;
    private final EventLineupRepository lineupRepository;
    private final EventSweaterRepository sweaterRepository;
    private final ChargeListRepository listRepository;
    private final ChampionshipRosterRepository rosterRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final UserProfileRepository profileRepository;
    private final NotificationService notificationService;

    public EventParticipationService(EventRepository eventRepository,
                                     EventVoteRepository voteRepository,
                                     EventLineupRepository lineupRepository,
                                     EventSweaterRepository sweaterRepository,
                                     ChargeListRepository listRepository,
                                     ChampionshipRosterRepository rosterRepository,
                                     TeamMemberRepository teamMemberRepository,
                                     TeamRepository teamRepository,
                                     UserProfileRepository profileRepository,
                                     NotificationService notificationService) {
        this.eventRepository = eventRepository;
        this.voteRepository = voteRepository;
        this.lineupRepository = lineupRepository;
        this.sweaterRepository = sweaterRepository;
        this.listRepository = listRepository;
        this.rosterRepository = rosterRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.teamRepository = teamRepository;
        this.profileRepository = profileRepository;
        this.notificationService = notificationService;
    }

    /** Удалить будущее участие и членство в списках, сохранив прошлое и финансы. */
    @Transactional
    public void removeFromTeam(Long teamId, Long userId) {
        List<Event> events = eventRepository.findByTeamIdAndStatusAndEventDateGreaterThanEqual(
                teamId, "ACTIVE", LocalDate.now());
        for (Event event : events) {
            removeFromEventIfFuture(event, userId, true);
        }

        for (ChargeList list : listRepository.findByTeamId(teamId)) {
            if (list.getMemberIds().remove(userId)) {
                listRepository.save(list);
            }
        }

        for (ChampionshipRoster roster : rosterRepository.findByTeamIdAndUserId(teamId, userId)) {
            if ("ACTIVE".equals(roster.getStatus())) {
                roster.setStatus("REMOVED");
                roster.setRemovedAt(OffsetDateTime.now());
                rosterRepository.save(roster);
            }
        }
    }

    /** Удалить голос и распределения только из будущих событий конкретного списка. */
    @Transactional
    public void removeFromList(Long listId, Long userId) {
        List<Event> events = eventRepository.findByVoterListIdAndStatusAndEventDateGreaterThanEqual(
                listId, "ACTIVE", LocalDate.now());
        for (Event event : events) {
            removeFromEventIfFuture(event, userId, false);
        }
    }

    /** После смены списка события удалить голоса людей, которых в нём больше нет. */
    @Transactional
    public void removeUsersNotInList(Event event, Set<Long> allowedUserIds) {
        if (!isActiveFuture(event)) return;
        Set<Long> voters = voteRepository.findByEventId(event.getId()).stream()
                .map(EventVote::getUserId)
                .filter(uid -> !allowedUserIds.contains(uid))
                .collect(Collectors.toSet());
        for (Long userId : voters) {
            removeFromEventIfFuture(event, userId, false);
        }
    }

    /** Удаление списка снимает ограничение голосования, не меняя уже поданные голоса. */
    @Transactional
    public void detachList(Long listId) {
        for (Event event : eventRepository.findByVoterListId(listId)) {
            event.setVoterListId(null);
            eventRepository.save(event);
        }
    }

    private void removeFromEventIfFuture(Event event, Long userId, boolean removeChargeRecipient) {
        if (!isActiveFuture(event)) return;

        Set<Long> mainBefore = mainSquadUserIds(event);
        voteRepository.deleteByEventIdAndUserId(event.getId(), userId);
        long lineupRemoved = lineupRepository.deleteByEventIdAndUserId(event.getId(), userId);
        long sweaterRemoved = sweaterRepository.deleteByEventIdAndUserId(event.getId(), userId);

        boolean eventChanged = false;
        if (lineupRemoved > 0) {
            event.setLineupRevision(event.getLineupRevision() + 1);
            eventChanged = true;
        }
        if (sweaterRemoved > 0) {
            event.setSweaterRevision(event.getSweaterRevision() + 1);
            eventChanged = true;
        }
        if (removeChargeRecipient && event.getChargeMemberIds().remove(userId)) {
            eventChanged = true;
        }
        if (eventChanged) eventRepository.save(event);

        voteRepository.flush();
        lineupRepository.flush();
        sweaterRepository.flush();

        Set<Long> promoted = new HashSet<>(mainSquadUserIds(event));
        promoted.removeAll(mainBefore);
        promoted.remove(userId);
        if (!promoted.isEmpty()) {
            String teamName = teamRepository.findById(event.getTeamId())
                    .map(Team::getName).orElse("команда");
            String when = event.getEventDate().format(DATE_FMT) + " " + event.getTimeOfDay();
            for (Long promotedUserId : promoted) {
                notificationService.notifyReservePromoted(promotedUserId, event.getTeamId(), teamName,
                        event.getId(), event.getTitle(), when);
            }
        }
    }

    private boolean isActiveFuture(Event event) {
        return "ACTIVE".equals(event.getStatus()) && !EventService.isEventPast(event);
    }

    private Set<Long> mainSquadUserIds(Event event) {
        Integer limit = event.getRequiredPlayers();
        if (limit == null || limit <= 0) return Set.of();
        List<EventVote> yesSorted = voteRepository.findByEventId(event.getId()).stream()
                .filter(v -> v.getAnswer() == VoteAnswer.YES)
                .sorted(Comparator.comparing(EventVote::getUpdatedAt))
                .toList();
        return mainSquadVotes(event, yesSorted, limit).stream()
                .map(EventVote::getUserId)
                .collect(Collectors.toSet());
    }

    private List<EventVote> mainSquadVotes(Event event, List<EventVote> yesSorted, int limit) {
        List<EventVote> outside = new ArrayList<>();
        List<EventVote> field = new ArrayList<>();
        for (EventVote vote : yesSorted) {
            boolean goalkeeper = profileRepository.findById(vote.getUserId())
                    .map(p -> p.getPositions().contains(UserPosition.GOALKEEPER))
                    .orElse(false);
            boolean trainer = teamMemberRepository
                    .findByTeamIdAndUserId(event.getTeamId(), vote.getUserId())
                    .map(m -> Boolean.TRUE.equals(m.getIsTrainer()))
                    .orElse(false);
            if (goalkeeper || trainer) outside.add(vote);
            else field.add(vote);
        }
        List<EventVote> result = new ArrayList<>(outside);
        result.addAll(field.stream().limit(limit).toList());
        return result;
    }
}
