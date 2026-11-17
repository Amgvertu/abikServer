package info.prorabka.abik.server.charge.service;

import info.prorabka.abik.server.charge.dto.SubscriptionPollResponse;
import info.prorabka.abik.server.charge.dto.SubscriptionPollVoteRequest;
import info.prorabka.abik.server.charge.entity.Charge;
import info.prorabka.abik.server.charge.entity.ChargeDistribution;
import info.prorabka.abik.server.charge.entity.ChargeTemplate;
import info.prorabka.abik.server.charge.entity.SubscriptionPoll;
import info.prorabka.abik.server.charge.entity.SubscriptionPollAnswer;
import info.prorabka.abik.server.charge.entity.SubscriptionPollVote;
import info.prorabka.abik.server.charge.repository.ChargeRepository;
import info.prorabka.abik.server.charge.repository.ChargeTemplateRepository;
import info.prorabka.abik.server.charge.repository.SubscriptionPollRepository;
import info.prorabka.abik.server.charge.repository.SubscriptionPollVoteRepository;
import info.prorabka.abik.server.common.exception.BadRequestException;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionPollServiceTest {

    @Mock SubscriptionPollRepository pollRepository;
    @Mock SubscriptionPollVoteRepository voteRepository;
    @Mock ChargeTemplateRepository templateRepository;
    @Mock ChargeRepository chargeRepository;
    @Mock TeamRepository teamRepository;
    @Mock TeamMemberRepository memberRepository;
    @Mock TeamMemberService memberService;
    @Mock UserProfileRepository profileRepository;
    @Mock RegionRepository regionRepository;
    @Mock NotificationService notificationService;

    @InjectMocks SubscriptionPollService service;

    @Test
    void shortMonthMovesPollDaysToLastDayAndSubscriptionToNextMonth() {
        Team team = team();
        ChargeTemplate template = template(30, 31, ChargeDistribution.FIXED, 30_001L);
        when(teamRepository.findByStatusOrderByNameAsc(TeamStatus.ACTIVE)).thenReturn(List.of(team));
        when(templateRepository.findByTeamIdAndActiveOrderByCreatedAtDesc(1L, true))
                .thenReturn(List.of(template));
        when(pollRepository.findByTemplateIdAndPeriodYearAndPeriodMonth(10L, (short) 2027, (short) 2))
                .thenReturn(Optional.empty());
        when(pollRepository.findByTeamIdAndProcessedAtIsNull(1L)).thenReturn(List.of());

        int created = service.process(LocalDate.of(2027, 2, 28));

        assertEquals(0, created);
        ArgumentCaptor<SubscriptionPoll> captor = ArgumentCaptor.forClass(SubscriptionPoll.class);
        verify(pollRepository).save(captor.capture());
        SubscriptionPoll poll = captor.getValue();
        assertEquals(LocalDate.of(2027, 2, 28), poll.getStartDate());
        assertEquals(LocalDate.of(2027, 2, 28), poll.getEndDate());
        assertEquals(LocalDate.of(2027, 3, 1), poll.getValidFrom());
        assertEquals(LocalDate.of(2027, 3, 31), poll.getValidUntil());
    }

    @Test
    void chargesOnlyPositiveFieldPlayersSplitsAmountAndDoesNotDuplicate() {
        Team team = team();
        ChargeTemplate template = template(20, 25, ChargeDistribution.EQUAL, 30_001L);
        SubscriptionPoll poll = poll(template, LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 25));
        when(teamRepository.findByStatusOrderByNameAsc(TeamStatus.ACTIVE)).thenReturn(List.of(team));
        when(templateRepository.findByTeamIdAndActiveOrderByCreatedAtDesc(1L, true))
                .thenReturn(List.of(template));
        when(pollRepository.findByTemplateIdAndPeriodYearAndPeriodMonth(10L, (short) 2026, (short) 11))
                .thenReturn(Optional.of(poll));
        when(pollRepository.findByTeamIdAndProcessedAtIsNull(1L))
                .thenReturn(List.of(poll), List.of());
        when(voteRepository.findByPollId(100L)).thenReturn(List.of(
                vote(1L, SubscriptionPollAnswer.YES),
                vote(2L, SubscriptionPollAnswer.YES),
                vote(3L, SubscriptionPollAnswer.YES),
                vote(4L, SubscriptionPollAnswer.YES),
                vote(5L, SubscriptionPollAnswer.NO)
        ));
        when(memberRepository.findByTeamIdAndStatusOrderByJoinedAtAsc(1L, MemberStatus.ACTIVE))
                .thenReturn(List.of(member(1L, false), member(2L, false), member(3L, false),
                        member(4L, true), member(5L, false)));
        when(profileRepository.findById(anyLong())).thenAnswer(invocation -> {
            Long userId = invocation.getArgument(0);
            Set<UserPosition> positions = userId == 3L
                    ? Set.of(UserPosition.GOALKEEPER) : Set.of(UserPosition.FORWARD);
            return Optional.of(UserProfile.builder().userId(userId).positions(positions).build());
        });
        when(chargeRepository.findByTemplateIdAndUserIdAndPeriodYearAndPeriodMonth(
                anyLong(), anyLong(), any(), any())).thenReturn(Optional.empty());
        when(chargeRepository.save(any(Charge.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int firstRun = service.process(LocalDate.of(2026, 11, 26));
        int secondRun = service.process(LocalDate.of(2026, 11, 26));

        assertEquals(2, firstRun);
        assertEquals(0, secondRun);
        ArgumentCaptor<Charge> captor = ArgumentCaptor.forClass(Charge.class);
        verify(chargeRepository, times(2)).save(captor.capture());
        assertEquals(Set.of(1L, 2L), captor.getAllValues().stream().map(Charge::getUserId)
                .collect(java.util.stream.Collectors.toSet()));
        captor.getAllValues().forEach(charge -> {
            assertEquals(20_000L, charge.getAmountKopecks());
            assertEquals((short) 2026, charge.getPeriodYear());
            assertEquals((short) 12, charge.getPeriodMonth());
            assertEquals(LocalDate.of(2026, 12, 1), charge.getDueDate());
        });
        assertNotNull(poll.getProcessedAt());
        verify(notificationService, times(2)).notifyNewCharge(anyLong(), anyLong(), any(), any(), anyLong(), any());
    }

    @Test
    void participantCanChangeVoteDuringPoll() {
        LocalDate today = LocalDate.now();
        ChargeTemplate template = template(1, 31, ChargeDistribution.FIXED, 10_000L);
        SubscriptionPoll poll = poll(template, today.minusDays(1), today.plusDays(1));
        SubscriptionPollVote existing = vote(1L, SubscriptionPollAnswer.YES);
        when(pollRepository.findById(100L)).thenReturn(Optional.of(poll));
        when(templateRepository.findById(10L)).thenReturn(Optional.of(template));
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team()));
        when(voteRepository.findByPollIdAndUserId(100L, 1L)).thenReturn(Optional.of(existing));
        when(voteRepository.findByPollId(100L)).thenReturn(List.of(existing));

        SubscriptionPollResponse response = service.vote(1L, 100L, 1L,
                new SubscriptionPollVoteRequest(SubscriptionPollAnswer.NO));

        verify(voteRepository).save(existing);
        assertEquals(SubscriptionPollAnswer.NO, existing.getAnswer());
        assertEquals(0, response.yesCount());
        assertEquals(1, response.noCount());
        assertEquals(SubscriptionPollAnswer.NO, response.myVote());
    }

    @Test
    void voteIsRejectedAfterPollEnd() {
        LocalDate today = LocalDate.now();
        ChargeTemplate template = template(1, 31, ChargeDistribution.FIXED, 10_000L);
        SubscriptionPoll poll = poll(template, today.minusDays(3), today.minusDays(1));
        when(pollRepository.findById(100L)).thenReturn(Optional.of(poll));
        when(templateRepository.findById(10L)).thenReturn(Optional.of(template));
        when(teamRepository.findById(1L)).thenReturn(Optional.of(team()));

        assertThrows(BadRequestException.class, () -> service.vote(1L, 100L, 1L,
                new SubscriptionPollVoteRequest(SubscriptionPollAnswer.YES)));
        verify(voteRepository, never()).save(any());
    }

    private Team team() {
        return Team.builder().id(1L).name("Abik").status(TeamStatus.ACTIVE).build();
    }

    private ChargeTemplate template(int startDay, int endDay, ChargeDistribution distribution, long amount) {
        return ChargeTemplate.builder()
                .id(10L).teamId(1L).groupId(20L).name("Абонемент (опрос)")
                .amountKopecks(amount).distribution(distribution).active(true)
                .pollEnabled(true).pollStartDay((short) startDay).pollEndDay((short) endDay)
                .build();
    }

    private SubscriptionPoll poll(ChargeTemplate template, LocalDate start, LocalDate end) {
        return SubscriptionPoll.builder()
                .id(100L).teamId(1L).templateId(template.getId()).groupId(template.getGroupId())
                .title(template.getName()).amountKopecks(template.getAmountKopecks())
                .distribution(template.getDistribution()).periodYear((short) 2026).periodMonth((short) 11)
                .startDate(start).endDate(end)
                .validFrom(LocalDate.of(2026, 12, 1)).validUntil(LocalDate.of(2026, 12, 31))
                .build();
    }

    private SubscriptionPollVote vote(Long userId, SubscriptionPollAnswer answer) {
        return SubscriptionPollVote.builder().pollId(100L).userId(userId).answer(answer).build();
    }

    private TeamMember member(Long userId, boolean trainer) {
        return TeamMember.builder().teamId(1L).userId(userId).status(MemberStatus.ACTIVE)
                .isTrainer(trainer).build();
    }
}
