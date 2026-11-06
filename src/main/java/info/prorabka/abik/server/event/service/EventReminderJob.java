package info.prorabka.abik.server.event.service;

import info.prorabka.abik.server.event.entity.Event;
import info.prorabka.abik.server.event.repository.EventRepository;
import info.prorabka.abik.server.notification.entity.EventReminderLog;
import info.prorabka.abik.server.notification.entity.NotificationType;
import info.prorabka.abik.server.notification.entity.UserNotificationSettings;
import info.prorabka.abik.server.notification.repository.EventReminderLogRepository;
import info.prorabka.abik.server.notification.repository.UserNotificationSettingsRepository;
import info.prorabka.abik.server.notification.service.NotificationService;
import info.prorabka.abik.server.team.entity.MemberStatus;
import info.prorabka.abik.server.team.entity.TeamMember;
import info.prorabka.abik.server.team.repository.TeamMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/** Персональные напоминания обо всех типах событий по настройкам каждого пользователя. */
@Component
public class EventReminderJob {

    private static final Logger log = LoggerFactory.getLogger(EventReminderJob.class);

    private final EventRepository eventRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final NotificationService notificationService;
    private final UserNotificationSettingsRepository settingsRepository;
    private final EventReminderLogRepository reminderLogRepository;

    public EventReminderJob(EventRepository eventRepository,
                            TeamMemberRepository teamMemberRepository,
                            NotificationService notificationService,
                            UserNotificationSettingsRepository settingsRepository,
                            EventReminderLogRepository reminderLogRepository) {
        this.eventRepository = eventRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.notificationService = notificationService;
        this.settingsRepository = settingsRepository;
        this.reminderLogRepository = reminderLogRepository;
    }

    /** Раз в минуту отправляет наступившие напоминания, не дублируя уже отправленные. */
    @Scheduled(cron = "0 * * * * *")
    public void sendPersonalReminders() {
        LocalDate today = LocalDate.now();
        List<Event> events = eventRepository.findByEventDateBetweenAndStatus(
                today, today.plusDays(31), "ACTIVE");
        LocalDateTime now = LocalDateTime.now();

        for (Event event : events) {
            try {
                processEvent(event, now);
            } catch (Exception ex) {
                log.error("Ошибка напоминания о событии {}: {}", event.getId(), ex.getMessage(), ex);
            }
        }
    }

    private void processEvent(Event event, LocalDateTime now) {
        LocalDateTime startsAt = LocalDateTime.of(event.getEventDate(), parseTime(event.getTimeOfDay()));
        if (!startsAt.isAfter(now)) return;

        List<TeamMember> members = teamMemberRepository.findByTeamIdAndStatusOrderByJoinedAtAsc(
                event.getTeamId(), MemberStatus.ACTIVE);
        for (TeamMember member : members) {
            UserNotificationSettings settings = settingsRepository.findById(member.getUserId())
                    .orElseGet(() -> UserNotificationSettings.defaults(member.getUserId()));
            if (!Boolean.TRUE.equals(settings.getRemindersEnabled())) continue;

            LocalDateTime remindAt = startsAt.minusMinutes(settings.getLeadTimeMinutes());
            if (remindAt.isAfter(now) || !remindAt.isBefore(startsAt)) continue;
            if (reminderLogRepository.existsByUserIdAndEventIdAndReminderIndex(
                    member.getUserId(), event.getId(), 0)) continue;

            OffsetDateTime scheduledFor = remindAt.atOffset(OffsetDateTime.now().getOffset());
            try {
                reminderLogRepository.save(EventReminderLog.builder()
                        .userId(member.getUserId())
                        .eventId(event.getId())
                        .reminderIndex(0)
                        .scheduledFor(scheduledFor)
                        .build());
            } catch (DataIntegrityViolationException ignored) {
                continue; // Параллельный планировщик уже зарегистрировал отправку.
            }

            String time = event.getTimeOfDay() == null ? "" : event.getTimeOfDay();
            String place = event.getPlace() == null || event.getPlace().isBlank()
                    ? "" : ", " + event.getPlace();
            notificationService.send(member.getUserId(), NotificationType.EVENT_REMINDER,
                    "Напоминание о событии",
                    event.getTitle() + " — " + event.getEventDate() + " в " + time + place,
                    event.getTeamId(),
                    Map.of("eventId", event.getId(), "kind", "EVENT_REMINDER",
                            "reminderIndex", 1));
        }
    }

    private LocalTime parseTime(String value) {
        try {
            return LocalTime.parse(value);
        } catch (Exception ex) {
            return LocalTime.MIDNIGHT;
        }
    }
}
