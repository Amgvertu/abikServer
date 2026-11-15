package info.prorabka.abik.server.event.service;

import info.prorabka.abik.server.charge.repository.ChargeListRepository;
import info.prorabka.abik.server.event.dto.EventListItemResponse;
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
import info.prorabka.abik.server.team.entity.MemberStatus;
import info.prorabka.abik.server.team.entity.TeamMember;
import info.prorabka.abik.server.team.repository.TeamMemberRepository;
import info.prorabka.abik.server.team.repository.TeamRepository;
import info.prorabka.abik.server.team.service.TeamMemberService;
import info.prorabka.abik.server.user.entity.UserPosition;
import info.prorabka.abik.server.user.entity.UserProfile;
import info.prorabka.abik.server.user.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceUniversalPositionTest {

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

    @Test
    void universalVoterIsCountedOnlyInPrimaryPosition() {
        Event event = Event.builder()
                .id(10L).teamId(1L).section(EventSection.TRAINING).type(EventType.TRAINING)
                .eventDate(LocalDate.now().plusDays(1)).timeOfDay("18:00")
                .title("Тренировка").place("Арена").status("ACTIVE").build();
        List<EventVote> votes = List.of(vote(100L), vote(200L), vote(300L));
        List<TeamMember> members = List.of(member(100L), member(200L), member(300L));

        when(eventRepo.findByTeamIdAndEventDateBetweenOrderByEventDateAscTimeOfDayAsc(
                anyLong(), any(), any())).thenReturn(List.of(event));
        when(teamMemberRepo.findByTeamIdAndStatusOrderByJoinedAtAsc(1L, MemberStatus.ACTIVE))
                .thenReturn(members);
        when(voteRepo.findByEventId(10L)).thenReturn(votes);
        when(voteRepo.countByEventIdAndAnswer(10L, VoteAnswer.YES)).thenReturn(3L);
        when(voteRepo.findByEventIdAndUserId(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(teamMemberRepo.findByTeamIdAndUserId(anyLong(), anyLong())).thenAnswer(invocation ->
                Optional.of(member(invocation.getArgument(1))));
        when(profileRepo.findById(100L)).thenReturn(Optional.of(profile(100L,
                Set.of(UserPosition.GOALKEEPER, UserPosition.FORWARD))));
        when(profileRepo.findById(200L)).thenReturn(Optional.of(profile(200L,
                Set.of(UserPosition.DEFENDER, UserPosition.FORWARD))));
        when(profileRepo.findById(300L)).thenReturn(Optional.of(profile(300L,
                Set.of(UserPosition.FORWARD))));
        when(lineupRepo.findByEventId(10L)).thenReturn(List.of());

        EventListItemResponse result = eventService.list(1L, 999L,
                LocalDate.now(), LocalDate.now().plusDays(2)).get(0);

        assertEquals(1, result.votesYesGoalies());
        assertEquals(1, result.votesYesDefenders());
        assertEquals(1, result.votesYesForwards());
        assertEquals(0, result.votesYesTrainers());
        assertEquals(3, result.votesYesGoalies() + result.votesYesDefenders()
                + result.votesYesForwards() + result.votesYesTrainers());
    }

    private EventVote vote(Long userId) {
        return EventVote.builder().eventId(10L).userId(userId)
                .answer(VoteAnswer.YES).updatedAt(OffsetDateTime.now()).build();
    }

    private TeamMember member(Long userId) {
        return TeamMember.builder().teamId(1L).userId(userId)
                .status(MemberStatus.ACTIVE).isTrainer(false).build();
    }

    private UserProfile profile(Long userId, Set<UserPosition> positions) {
        return UserProfile.builder().userId(userId).positions(positions).build();
    }
}
