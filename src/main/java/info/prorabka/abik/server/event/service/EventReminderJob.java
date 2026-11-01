package info.prorabka.abik.server.event.service;

import info.prorabka.abik.server.event.entity.Event;
import info.prorabka.abik.server.event.entity.EventSection;
import info.prorabka.abik.server.event.repository.EventRepository;
import info.prorabka.abik.server.notification.entity.NotificationType;
import info.prorabka.abik.server.notification.service.NotificationService;
import info.prorabka.abik.server.team.entity.MemberStatus;
import info.prorabka.abik.server.team.entity.TeamMember;
import info.prorabka.abik.server.team.repository.TeamMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Напоминание о тренировке накануне: каждый день в 20:00 (по времени сервера)
 * всем активным участникам команды уходит уведомление о завтрашних
 * тренировках с указанием времени и места.
 */
@Component
public class EventReminderJob {

    private static final Logger log = LoggerFactory.getLogger(EventReminderJob.class);

    private final EventRepository eventRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final NotificationService notificationService;

    public EventReminderJob(EventRepository eventRepository,
                            TeamMemberRepository teamMemberRepository,
                            NotificationService notificationService) {
        this.eventRepository = eventRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.notificationService = notificationService;
    }

    /** Ежедневно в 20:00 по серверному времени. */
    @Scheduled(cron = "0 0 20 * * *")
    public void sendTomorrowTrainingReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Event> events = eventRepository.findByEventDateAndSectionAndStatus(
                tomorrow, EventSection.TRAINING, "ACTIVE");
        if (events.isEmpty()) return;

        log.info("Напоминания о тренировках на {}: {} событий", tomorrow, events.size());
        for (Event e : events) {
            try {
                String time = e.getTimeOfDay() == null ? "" : e.getTimeOfDay();
                String place = e.getPlace() == null ? "" : e.getPlace();
                String title = "Тренировка завтра";
                String body = "Завтра тренировка в " + time
                        + (place.isBlank() ? "" : ", " + place);

                List<TeamMember> members = teamMemberRepository
                        .findByTeamIdAndStatusOrderByJoinedAtAsc(e.getTeamId(), MemberStatus.ACTIVE);
                for (TeamMember m : members) {
                    notificationService.send(m.getUserId(), NotificationType.EVENT_REMINDER,
                            title, body, e.getTeamId(),
                            Map.of("eventId", e.getId(), "kind", "EVENT_REMINDER"));
                }
            } catch (Exception ex) {
                log.error("Ошибка напоминания о событии {}: {}", e.getId(), ex.getMessage(), ex);
            }
        }
    }
}
