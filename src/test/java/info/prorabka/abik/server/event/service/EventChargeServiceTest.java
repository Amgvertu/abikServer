package info.prorabka.abik.server.event.service;

import info.prorabka.abik.server.charge.entity.Charge;
import info.prorabka.abik.server.charge.entity.ChargeGroup;
import info.prorabka.abik.server.charge.repository.ChargeGroupRepository;
import info.prorabka.abik.server.charge.repository.ChargeRepository;
import info.prorabka.abik.server.charge.repository.ChargeTemplateRepository;
import info.prorabka.abik.server.event.entity.Event;
import info.prorabka.abik.server.event.entity.EventSection;
import info.prorabka.abik.server.event.entity.EventType;
import info.prorabka.abik.server.event.entity.EventVote;
import info.prorabka.abik.server.event.entity.VoteAnswer;
import info.prorabka.abik.server.event.repository.EventLineupRepository;
import info.prorabka.abik.server.event.repository.EventRepository;
import info.prorabka.abik.server.event.repository.EventVoteRepository;
import info.prorabka.abik.server.notification.service.NotificationService;
import info.prorabka.abik.server.team.entity.Team;
import info.prorabka.abik.server.team.entity.TeamMember;
import info.prorabka.abik.server.team.repository.TeamMemberRepository;
import info.prorabka.abik.server.team.repository.TeamRepository;
import info.prorabka.abik.server.team.service.TeamMemberService;
import info.prorabka.abik.server.user.entity.UserPosition;
import info.prorabka.abik.server.user.entity.UserProfile;
import info.prorabka.abik.server.user.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventChargeServiceTest {

    @Mock EventRepository eventRepository;
    @Mock EventVoteRepository voteRepository;
    @Mock EventLineupRepository lineupRepository;
    @Mock ChargeRepository chargeRepository;
    @Mock ChargeGroupRepository groupRepository;
    @Mock ChargeTemplateRepository templateRepository;
    @Mock TeamMemberService teamMemberService;
    @Mock TeamRepository teamRepository;
    @Mock TeamMemberRepository teamMemberRepository;
    @Mock NotificationService notificationService;
    @Mock UserProfileRepository profileRepository;

    @InjectMocks EventChargeService eventChargeService;

    @Test
    void voteChargeExcludesGoalkeepersTrainersAndReserve() {
        Event event = voteEvent("EACH_ATTENDED", 1, 10_000L);
        prepareEvent(event, List.of(
                vote(1L, VoteAnswer.YES, 1),
                vote(2L, VoteAnswer.YES, 2),
                vote(3L, VoteAnswer.YES, 3),
                vote(4L, VoteAnswer.YES, 4),
                vote(5L, VoteAnswer.NO, 5)
        ));
        prepareMembersAndProfiles();

        Map<String, Object> result = eventChargeService.doCharge(1L, 10L);

        assertEquals(1, result.get("count"));
        assertEquals(10_000L, result.get("perPersonKopecks"));
        ArgumentCaptor<Charge> captor = ArgumentCaptor.forClass(Charge.class);
        verify(chargeRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getUserId());
        assertEquals(10_000L, captor.getValue().getAmountKopecks());
        assertNotNull(event.getChargedAt());
    }

    @Test
    void voteChargeSplitsAmountBetweenMainSquadFieldPlayers() {
        Event event = voteEvent("SPLIT_ATTENDED", 2, 10_001L);
        prepareEvent(event, List.of(
                vote(1L, VoteAnswer.YES, 1),
                vote(2L, VoteAnswer.YES, 2)
        ));
        prepareMembersAndProfiles();

        Map<String, Object> result = eventChargeService.doCharge(1L, 10L);

        assertEquals(2, result.get("count"));
        assertEquals(5_001L, result.get("perPersonKopecks"));
        ArgumentCaptor<Charge> captor = ArgumentCaptor.forClass(Charge.class);
        verify(chargeRepository, times(2)).save(captor.capture());
        assertEquals(Set.of(1L, 2L), captor.getAllValues().stream()
                .map(Charge::getUserId)
                .collect(java.util.stream.Collectors.toSet()));
        captor.getAllValues().forEach(charge -> assertEquals(5_001L, charge.getAmountKopecks()));
    }

    @Test
    void scrimmageVoteChargeExcludesSubscriptionHoldersBeforeSplitting() {
        Event event = voteEvent("SPLIT_ATTENDED", 2, 20_001L);
        event.setExcludeSubscriptionHolders(true);
        prepareEvent(event, List.of(
                vote(1L, VoteAnswer.YES, 1),
                vote(2L, VoteAnswer.YES, 2)
        ));
        prepareMembersAndProfiles();
        when(chargeRepository.findSubscriptionHolderIds(1L, (short) 2026, (short) 11,
                List.of(info.prorabka.abik.server.charge.entity.ChargeStatus.ACTIVE,
                        info.prorabka.abik.server.charge.entity.ChargeStatus.PARTIALLY_PAID,
                        info.prorabka.abik.server.charge.entity.ChargeStatus.PAID)))
                .thenReturn(Set.of(1L));

        Map<String, Object> result = eventChargeService.doCharge(1L, 10L);

        assertEquals(1, result.get("count"));
        assertEquals(20_001L, result.get("perPersonKopecks"));
        ArgumentCaptor<Charge> captor = ArgumentCaptor.forClass(Charge.class);
        verify(chargeRepository).save(captor.capture());
        assertEquals(2L, captor.getValue().getUserId());
        assertEquals(20_001L, captor.getValue().getAmountKopecks());
    }

    private Event voteEvent(String distribution, int requiredPlayers, long amountKopecks) {
        return Event.builder()
                .id(10L).teamId(1L).title("Двухсторонка")
                .section(EventSection.SCRIMMAGE).type(EventType.GAME)
                .eventDate(LocalDate.of(2026, 11, 17)).timeOfDay("21:00")
                .chargeMode("VOTE").chargeAmountKopecks(amountKopecks)
                .chargeDistribution(distribution).requiredPlayers(requiredPlayers)
                .build();
    }

    private void prepareEvent(Event event, List<EventVote> votes) {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(teamRepository.findById(1L)).thenReturn(Optional.of(Team.builder().id(1L).name("Abik").build()));
        when(voteRepository.findByEventId(10L)).thenReturn(votes);
        when(groupRepository.findByTeamIdAndName(1L, "Двухсторонка"))
                .thenReturn(Optional.of(ChargeGroup.builder().id(77L).teamId(1L).name("Двухсторонка").build()));
        when(chargeRepository.save(any(Charge.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void prepareMembersAndProfiles() {
        when(teamMemberRepository.findByTeamIdAndUserId(anyLong(), anyLong())).thenAnswer(invocation -> {
            Long userId = invocation.getArgument(1);
            return Optional.of(TeamMember.builder()
                    .teamId(1L).userId(userId).isTrainer(userId == 4L).build());
        });
        when(profileRepository.findById(anyLong())).thenAnswer(invocation -> {
            Long userId = invocation.getArgument(0);
            Set<UserPosition> positions = userId == 3L
                    ? Set.of(UserPosition.GOALKEEPER)
                    : Set.of(UserPosition.FORWARD);
            return Optional.of(UserProfile.builder().userId(userId).positions(positions).build());
        });
    }

    private EventVote vote(Long userId, VoteAnswer answer, int minute) {
        return EventVote.builder()
                .eventId(10L).userId(userId).answer(answer)
                .updatedAt(OffsetDateTime.parse("2026-11-17T12:00:00Z").plusMinutes(minute))
                .build();
    }
}
