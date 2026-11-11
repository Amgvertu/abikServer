package info.prorabka.abik.server.notification.service;

import info.prorabka.abik.server.notification.entity.HolidayNotificationLog;
import info.prorabka.abik.server.notification.entity.NotificationType;
import info.prorabka.abik.server.notification.repository.HolidayNotificationLogRepository;
import info.prorabka.abik.server.team.entity.MemberStatus;
import info.prorabka.abik.server.team.entity.Team;
import info.prorabka.abik.server.team.entity.TeamMember;
import info.prorabka.abik.server.team.repository.TeamMemberRepository;
import info.prorabka.abik.server.team.repository.TeamRepository;
import info.prorabka.abik.server.user.entity.UserPosition;
import info.prorabka.abik.server.user.entity.UserProfile;
import info.prorabka.abik.server.user.repository.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** Напоминания команде накануне и персональные поздравления в день праздника. */
@Component
public class HolidayNotificationJob {

    private static final Logger log = LoggerFactory.getLogger(HolidayNotificationJob.class);
    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd.MM");
    private static final LocalTime REMINDER_TIME = LocalTime.of(18, 0);
    private static final LocalTime GREETING_TIME = LocalTime.of(9, 0);

    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final UserProfileRepository profileRepository;
    private final HolidayNotificationLogRepository logRepository;
    private final NotificationService notificationService;

    public HolidayNotificationJob(TeamMemberRepository teamMemberRepository,
                                  TeamRepository teamRepository,
                                  UserProfileRepository profileRepository,
                                  HolidayNotificationLogRepository logRepository,
                                  NotificationService notificationService) {
        this.teamMemberRepository = teamMemberRepository;
        this.teamRepository = teamRepository;
        this.profileRepository = profileRepository;
        this.logRepository = logRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 * * * * *")
    public void sendScheduledNotifications() {
        process(LocalDate.now(), LocalTime.now());
    }

    /** Если сервер был временно остановлен, отправка выполняется позже в тот же день. */
    void process(LocalDate today, LocalTime now) {
        List<TeamMember> activeMemberships = teamMemberRepository.findByStatus(MemberStatus.ACTIVE);
        if (!now.isBefore(GREETING_TIME)) {
            for (HolidayKind kind : HolidayKind.values()) {
                processHoliday(today, Stage.GREETING, kind, activeMemberships);
            }
        }
        if (!now.isBefore(REMINDER_TIME)) {
            LocalDate tomorrow = today.plusDays(1);
            for (HolidayKind kind : HolidayKind.values()) {
                processHoliday(tomorrow, Stage.REMINDER, kind, activeMemberships);
            }
        }
    }

    private void processHoliday(LocalDate holidayDate, Stage stage, HolidayKind kind,
                                List<TeamMember> activeMemberships) {
        for (TeamMember celebrated : activeMemberships) {
            try {
                if (!matches(celebrated, kind, holidayDate)) continue;
                if (stage == Stage.GREETING) {
                    send(celebrated, celebrated, holidayDate, stage, kind);
                } else {
                    List<TeamMember> recipients = teamMemberRepository
                            .findByTeamIdAndStatusOrderByJoinedAtAsc(
                                    celebrated.getTeamId(), MemberStatus.ACTIVE);
                    for (TeamMember recipient : recipients) {
                        send(recipient, celebrated, holidayDate, stage, kind);
                    }
                }
            } catch (Exception ex) {
                log.error("Ошибка праздничного уведомления: teamId={}, userId={}, тип={}, этап={}: {}",
                        celebrated.getTeamId(), celebrated.getUserId(), kind, stage,
                        ex.getMessage(), ex);
            }
        }
    }

    private boolean matches(TeamMember member, HolidayKind kind, LocalDate holidayDate) {
        if (kind == HolidayKind.COACH_DAY) {
            return holidayDate.getMonthValue() == 10 && holidayDate.getDayOfMonth() == 30
                    && Boolean.TRUE.equals(member.getIsTrainer());
        }
        if (kind == HolidayKind.GOALKEEPER_DAY) {
            if (holidayDate.getMonthValue() != 10 || holidayDate.getDayOfMonth() != 31
                    || Boolean.TRUE.equals(member.getIsTrainer())) {
                return false;
            }
            return profileRepository.findById(member.getUserId())
                    .map(p -> p.getPositions().contains(UserPosition.GOALKEEPER))
                    .orElse(false);
        }
        return profileRepository.findById(member.getUserId())
                .map(UserProfile::getBirthDate)
                .map(date -> date.getMonthValue() == holidayDate.getMonthValue()
                        && date.getDayOfMonth() == holidayDate.getDayOfMonth())
                .orElse(false);
    }

    private void send(TeamMember recipient, TeamMember celebrated, LocalDate holidayDate,
                      Stage stage, HolidayKind kind) {
        if (!register(recipient, celebrated, holidayDate, stage, kind)) return;

        String teamName = teamRepository.findById(celebrated.getTeamId())
                .map(Team::getName).orElse("команда");
        String celebratedName = displayName(celebrated.getUserId());
        boolean greeting = stage == Stage.GREETING;
        String title = greeting ? kind.greetingTitle : kind.reminderTitle;
        String body = greeting
                ? "Команда «" + teamName + "» поздравляет вас " + kind.greeting + "!"
                : "Завтра, " + holidayDate.format(DAY_MONTH) + ", " + kind.reminder
                        + ": " + celebratedName + ". Команда «" + teamName + "».";

        notificationService.send(recipient.getUserId(),
                greeting ? NotificationType.HOLIDAY_GREETING : NotificationType.HOLIDAY_REMINDER,
                title, body, celebrated.getTeamId(),
                Map.of("kind", greeting ? "HOLIDAY_GREETING" : "HOLIDAY_REMINDER",
                        "holidayType", kind.name(),
                        "celebratedUserId", celebrated.getUserId(),
                        "holidayDate", holidayDate.toString()));
    }

    private boolean register(TeamMember recipient, TeamMember celebrated, LocalDate holidayDate,
                             Stage stage, HolidayKind kind) {
        try {
            logRepository.save(HolidayNotificationLog.builder()
                    .recipientUserId(recipient.getUserId())
                    .celebratedUserId(celebrated.getUserId())
                    .teamId(celebrated.getTeamId())
                    .holidayType(kind.name())
                    .stage(stage.name())
                    .holidayYear(holidayDate.getYear())
                    .build());
            return true;
        } catch (DataIntegrityViolationException ignored) {
            return false;
        }
    }

    private String displayName(Long userId) {
        return profileRepository.findById(userId)
                .map(p -> (p.getLastName() + " " + p.getFirstName()).trim())
                .filter(name -> !name.isBlank())
                .orElse("участник #" + userId);
    }

    private enum Stage { REMINDER, GREETING }

    private enum HolidayKind {
        BIRTHDAY("Напоминание о дне рождения", "С днём рождения!",
                "день рождения", "с днём рождения"),
        COACH_DAY("Напоминание о Дне тренера", "С Днём тренера!",
                "День тренера", "с Днём тренера"),
        GOALKEEPER_DAY("Напоминание о Дне вратаря", "С Днём вратаря!",
                "День вратаря", "с Днём вратаря");

        private final String reminderTitle;
        private final String greetingTitle;
        private final String reminder;
        private final String greeting;

        HolidayKind(String reminderTitle, String greetingTitle, String reminder, String greeting) {
            this.reminderTitle = reminderTitle;
            this.greetingTitle = greetingTitle;
            this.reminder = reminder;
            this.greeting = greeting;
        }
    }
}
