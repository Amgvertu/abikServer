package info.prorabka.abik.server.event.service;

import info.prorabka.abik.server.charge.entity.ChargeList;
import info.prorabka.abik.server.charge.repository.ChargeListRepository;
import info.prorabka.abik.server.common.exception.ForbiddenException;
import info.prorabka.abik.server.event.entity.Event;
import info.prorabka.abik.server.event.entity.EventSection;
import info.prorabka.abik.server.event.entity.EventType;
import info.prorabka.abik.server.event.entity.EventVote;
import info.prorabka.abik.server.event.entity.VoteAnswer;
import info.prorabka.abik.server.event.repository.EventLineupRepository;
import info.prorabka.abik.server.event.repository.EventRepository;
import info.prorabka.abik.server.event.repository.EventSeriesRepository;
import info.prorabka.abik.server.event.repository.EventSweaterRepository;
import info.prorabka.abik.server.event.repository.EventVoteRepository;
import info.prorabka.abik.server.notification.service.NotificationService;
import info.prorabka.abik.server.roster.repository.ChampionshipRosterRepository;
import info.prorabka.abik.server.team.entity.TeamMember;
import info.prorabka.abik.server.team.repository.TeamMemberRepository;
import info.prorabka.abik.server.team.repository.TeamRepository;
import info.prorabka.abik.server.team.service.TeamMemberService;
import info.prorabka.abik.server.user.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceVoterListTest {

    @Mock EventRepository eventRepo;
    @Mock EventSeriesRepository seriesRepo;
    @Mock EventVoteRepository voteRepo;
    @Mock EventSweaterRepository sweaterRepo;
    @Mock EventLineupRepository lineupRepo;
    @Mock TeamMemberService teamMemberService;
    @Mock TeamMemberRepository teamMemberRepo;
    @Mock TeamRepository teamRepo;
    @Mock UserProfileRepository profileRepo;
    @Mock ChampionshipRosterRepository rosterRepo;
    @Mock NotificationService notificationService;
    @Mock ChargeListRepository chargeListRepo;
    @Mock EventParticipationService participationService;

    @InjectMocks EventService eventService;

    private Event event;

    @BeforeEach
    void setUp() {
        event = Event.builder()
                .id(10L).teamId(1L).section(EventSection.TRAINING).type(EventType.TRAINING)
                .eventDate(LocalDate.now().plusDays(1)).timeOfDay("18:00")
                .title("Тренировка").place("Арена").status("ACTIVE").voterListId(5L)
                .build();
        when(teamMemberService.requireMember(1L, 100L)).thenReturn(TeamMember.builder().build());
        when(eventRepo.findById(10L)).thenReturn(Optional.of(event));
    }

    @Test
    void memberOfCurrentListCanVote() {
        ChargeList list = ChargeList.builder().id(5L).teamId(1L)
                .memberIds(new HashSet<>(Set.of(100L, 200L))).build();
        when(chargeListRepo.findById(5L)).thenReturn(Optional.of(list));
        when(voteRepo.findByEventIdAndUserId(10L, 100L)).thenReturn(Optional.empty());

        eventService.vote(1L, 10L, 100L, VoteAnswer.YES);

        verify(voteRepo).save(any(EventVote.class));
    }

    @Test
    void userOutsideCurrentListCannotVote() {
        ChargeList list = ChargeList.builder().id(5L).teamId(1L)
                .memberIds(new HashSet<>(Set.of(200L))).build();
        when(chargeListRepo.findById(5L)).thenReturn(Optional.of(list));

        assertThrows(ForbiddenException.class,
                () -> eventService.vote(1L, 10L, 100L, VoteAnswer.YES));

        verify(voteRepo, never()).save(any(EventVote.class));
    }
}
