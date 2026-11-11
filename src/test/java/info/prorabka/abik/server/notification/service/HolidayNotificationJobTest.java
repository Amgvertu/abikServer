package info.prorabka.abik.server.notification.service;

import info.prorabka.abik.server.notification.entity.HolidayNotificationLog;
import info.prorabka.abik.server.notification.entity.NotificationType;
import info.prorabka.abik.server.notification.repository.HolidayNotificationLogRepository;
import info.prorabka.abik.server.team.entity.MemberStatus;
import info.prorabka.abik.server.team.entity.Team;
import info.prorabka.abik.server.team.entity.TeamMember;
import info.prorabka.abik.server.team.repository.TeamMemberRepository;
import info.prorabka.abik.server.team.repository.TeamRepository;
import info.prorabka.abik.server.user.entity.UserProfile;
import info.prorabka.abik.server.user.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HolidayNotificationJobTest {

    @Mock TeamMemberRepository teamMemberRepository;
    @Mock TeamRepository teamRepository;
    @Mock UserProfileRepository profileRepository;
    @Mock HolidayNotificationLogRepository logRepository;
    @Mock NotificationService notificationService;
    @InjectMocks HolidayNotificationJob job;

    @Test
    void sendsTeamReminderOnceAndPersonalGreetingOnlyToCelebrant() {
        TeamMember celebrant = TeamMember.builder().teamId(7L).userId(1L)
                .status(MemberStatus.ACTIVE).build();
        TeamMember other = TeamMember.builder().teamId(7L).userId(2L)
                .status(MemberStatus.ACTIVE).build();
        when(teamMemberRepository.findByStatus(MemberStatus.ACTIVE)).thenReturn(List.of(celebrant, other));
        when(teamMemberRepository.findByTeamIdAndStatusOrderByJoinedAtAsc(7L, MemberStatus.ACTIVE))
                .thenReturn(List.of(celebrant, other));
        when(profileRepository.findById(1L)).thenReturn(Optional.of(UserProfile.builder()
                .firstName("Иван").lastName("Иванов").birthDate(LocalDate.of(1990, 11, 12)).build()));
        when(profileRepository.findById(2L)).thenReturn(Optional.empty());
        when(teamRepository.findById(7L)).thenReturn(Optional.of(Team.builder().name("Команда").build()));

        job.process(LocalDate.of(2026, 11, 11), LocalTime.of(18, 0));

        verify(notificationService, times(2)).send(anyLong(), eq(NotificationType.HOLIDAY_REMINDER),
                anyString(), anyString(), eq(7L), anyMap());

        doThrow(new DataIntegrityViolationException("duplicate")).when(logRepository).save(any());
        job.process(LocalDate.of(2026, 11, 11), LocalTime.of(18, 1));
        verify(notificationService, times(2)).send(anyLong(), eq(NotificationType.HOLIDAY_REMINDER),
                anyString(), anyString(), eq(7L), anyMap());

        doAnswer(inv -> null).when(logRepository).save(any(HolidayNotificationLog.class));
        job.process(LocalDate.of(2026, 11, 12), LocalTime.of(9, 0));
        verify(notificationService).send(eq(1L), eq(NotificationType.HOLIDAY_GREETING),
                anyString(), anyString(), eq(7L), anyMap());
        verify(notificationService, never()).send(eq(2L), eq(NotificationType.HOLIDAY_GREETING),
                anyString(), anyString(), eq(7L), anyMap());
    }

    @Test
    void doesNotSendBeforeConfiguredTime() {
        when(teamMemberRepository.findByStatus(MemberStatus.ACTIVE)).thenReturn(List.of());

        job.process(LocalDate.of(2026, 11, 11), LocalTime.of(8, 59));

        verify(notificationService, never()).send(anyLong(), any(), anyString(), anyString(),
                anyLong(), anyMap());
    }
}
