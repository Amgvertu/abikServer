package info.prorabka.abik.server.event.service;

import info.prorabka.abik.server.charge.entity.ChargeList;
import info.prorabka.abik.server.charge.repository.ChargeListRepository;
import info.prorabka.abik.server.event.entity.Event;
import info.prorabka.abik.server.event.entity.EventSection;
import info.prorabka.abik.server.event.entity.EventType;
import info.prorabka.abik.server.event.entity.EventVote;
import info.prorabka.abik.server.event.entity.VoteAnswer;
import info.prorabka.abik.server.event.repository.EventLineupRepository;
import info.prorabka.abik.server.event.repository.EventRepository;
import info.prorabka.abik.server.event.repository.EventSweaterRepository;
import info.prorabka.abik.server.event.repository.EventVoteRepository;
import info.prorabka.abik.server.notification.service.NotificationService;
import info.prorabka.abik.server.roster.entity.ChampionshipRoster;
import info.prorabka.abik.server.roster.repository.ChampionshipRosterRepository;
import info.prorabka.abik.server.team.entity.Team;
import info.prorabka.abik.server.team.repository.TeamMemberRepository;
import info.prorabka.abik.server.team.repository.TeamRepository;
import info.prorabka.abik.server.user.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventParticipationServiceTest {

    @Mock EventRepository eventRepository;
    @Mock EventVoteRepository voteRepository;
    @Mock EventLineupRepository lineupRepository;
    @Mock EventSweaterRepository sweaterRepository;
    @Mock ChargeListRepository listRepository;
    @Mock ChampionshipRosterRepository rosterRepository;
    @Mock TeamMemberRepository teamMemberRepository;
    @Mock TeamRepository teamRepository;
    @Mock UserProfileRepository profileRepository;
    @Mock NotificationService notificationService;
    @InjectMocks EventParticipationService service;

    @Test
    void teamRemovalCleansFutureParticipationAndPromotesReserve() {
        Event event = Event.builder()
                .id(1L).teamId(7L).section(EventSection.REGULAR).type(EventType.GAME)
                .eventDate(LocalDate.now().plusDays(1)).timeOfDay("18:00")
                .title("Игра").place("Арена").status("ACTIVE").requiredPlayers(1)
                .lineupRevision(0L).sweaterRevision(0L)
                .chargeMemberIds(new HashSet<>(Set.of(2L))).build();
        EventVote removed = vote(2L, OffsetDateTime.now().minusMinutes(2));
        EventVote reserve = vote(3L, OffsetDateTime.now().minusMinutes(1));
        ChargeList list = ChargeList.builder().teamId(7L)
                .memberIds(new HashSet<>(Set.of(2L, 3L))).build();
        ChampionshipRoster roster = ChampionshipRoster.builder()
                .teamId(7L).userId(2L).status("ACTIVE").build();

        when(eventRepository.findByTeamIdAndStatusAndEventDateGreaterThanEqual(
                eq(7L), eq("ACTIVE"), anyLongDate())).thenReturn(List.of(event));
        when(voteRepository.findByEventId(1L))
                .thenReturn(List.of(removed, reserve))
                .thenReturn(List.of(reserve));
        when(lineupRepository.deleteByEventIdAndUserId(1L, 2L)).thenReturn(1L);
        when(sweaterRepository.deleteByEventIdAndUserId(1L, 2L)).thenReturn(1L);
        when(listRepository.findByTeamId(7L)).thenReturn(List.of(list));
        when(rosterRepository.findByTeamIdAndUserId(7L, 2L)).thenReturn(List.of(roster));
        when(profileRepository.findById(anyLong())).thenReturn(Optional.empty());
        when(teamMemberRepository.findByTeamIdAndUserId(eq(7L), anyLong())).thenReturn(Optional.empty());
        when(teamRepository.findById(7L)).thenReturn(Optional.of(Team.builder().name("Команда").build()));

        service.removeFromTeam(7L, 2L);

        verify(voteRepository).deleteByEventIdAndUserId(1L, 2L);
        assertEquals(1L, event.getLineupRevision());
        assertEquals(1L, event.getSweaterRevision());
        assertFalse(event.getChargeMemberIds().contains(2L));
        assertFalse(list.getMemberIds().contains(2L));
        assertEquals("REMOVED", roster.getStatus());
        verify(notificationService).notifyReservePromoted(eq(3L), eq(7L), eq("Команда"),
                eq(1L), eq("Игра"), anyString());
    }

    private EventVote vote(Long userId, OffsetDateTime updatedAt) {
        return EventVote.builder().eventId(1L).userId(userId)
                .answer(VoteAnswer.YES).updatedAt(updatedAt).build();
    }

    private LocalDate anyLongDate() {
        return org.mockito.ArgumentMatchers.any(LocalDate.class);
    }
}
