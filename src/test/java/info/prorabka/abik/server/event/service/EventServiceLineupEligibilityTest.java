package info.prorabka.abik.server.event.service;

import info.prorabka.abik.server.charge.entity.ChargeList;
import info.prorabka.abik.server.charge.repository.ChargeListRepository;
import info.prorabka.abik.server.common.exception.BadRequestException;
import info.prorabka.abik.server.event.dto.LineupSaveRequest;
import info.prorabka.abik.server.event.dto.LineupSlotDto;
import info.prorabka.abik.server.event.entity.Event;
import info.prorabka.abik.server.event.entity.EventSection;
import info.prorabka.abik.server.event.entity.EventType;
import info.prorabka.abik.server.event.entity.EventVote;
import info.prorabka.abik.server.event.entity.LineupRole;
import info.prorabka.abik.server.event.entity.VoteAnswer;
import info.prorabka.abik.server.event.repository.EventLineupRepository;
import info.prorabka.abik.server.event.repository.EventRepository;
import info.prorabka.abik.server.event.repository.EventSeriesRepository;
import info.prorabka.abik.server.event.repository.EventSweaterRepository;
import info.prorabka.abik.server.event.repository.EventVoteRepository;
import info.prorabka.abik.server.notification.service.NotificationService;
import info.prorabka.abik.server.roster.repository.ChampionshipRosterRepository;
import info.prorabka.abik.server.team.entity.MemberStatus;
import info.prorabka.abik.server.team.entity.Team;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceLineupEligibilityTest {
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
    @InjectMocks EventService service;

    private Event event;

    @BeforeEach
    void setUp() {
        event = Event.builder()
                .id(10L).teamId(1L).section(EventSection.REGULAR).type(EventType.GAME)
                .eventDate(LocalDate.now().plusDays(1)).timeOfDay("20:00")
                .title("Игра").place("Арена").status("ACTIVE").lineupRevision(0L)
                .build();
        when(eventRepo.findById(10L)).thenReturn(Optional.of(event));
        when(teamRepo.findById(1L)).thenReturn(Optional.of(Team.builder().id(1L).season("2026/27").build()));
        when(teamMemberRepo.findByTeamIdAndUserIdAndStatus(1L, 12L, MemberStatus.ACTIVE))
                .thenReturn(Optional.of(TeamMember.builder()
                        .teamId(1L).userId(12L).status(MemberStatus.ACTIVE).isTrainer(false).build()));
        when(voteRepo.findByEventIdAndUserId(10L, 12L))
                .thenReturn(Optional.of(EventVote.builder().eventId(10L).userId(12L).answer(VoteAnswer.YES).build()));
    }

    @Test
    void activeTeamMemberCanSaveWithoutListAndSeasonRoster() {
        assertDoesNotThrow(this::saveLineup);

        verify(lineupRepo).save(any());
        verifyNoInteractions(rosterRepo);
    }

    @Test
    void memberOfEventListCanSaveWithoutSeasonRoster() {
        event.setVoterListId(5L);
        when(chargeListRepo.findById(5L)).thenReturn(Optional.of(
                ChargeList.builder().id(5L).teamId(1L).memberIds(Set.of(12L)).build()));

        assertDoesNotThrow(this::saveLineup);

        verify(lineupRepo).save(any());
        verifyNoInteractions(rosterRepo);
    }

    @Test
    void userOutsideEventListCannotSave() {
        event.setVoterListId(5L);
        when(chargeListRepo.findById(5L)).thenReturn(Optional.of(
                ChargeList.builder().id(5L).teamId(1L).memberIds(Set.of(99L)).build()));

        assertThrows(BadRequestException.class, this::saveLineup);

        verify(lineupRepo, never()).save(any());
    }

    private void saveLineup() {
        service.setLineup(1L, 10L, 100L, new LineupSaveRequest(List.of(
                new LineupSlotDto(1, 1, LineupRole.FORWARD, 12L))));
    }
}
