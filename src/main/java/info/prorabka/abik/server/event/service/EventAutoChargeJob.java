package info.prorabka.abik.server.event.service;

import info.prorabka.abik.server.event.entity.Event;
import info.prorabka.abik.server.event.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Автоначисление за завершившиеся события (тренировки, регулярка,
 * двухсторонка). Момент окончания = указанные в событии дата и время.
 * Каждые 15 минут находим события, которые уже закончились, у которых
 * настроено начисление (MANUAL/TEMPLATE) и оно ещё не выполнялось,
 * и проводим его. Повторное начисление исключено флагом chargedAt.
 */
@Component
public class EventAutoChargeJob {

    private static final Logger log = LoggerFactory.getLogger(EventAutoChargeJob.class);

    private final EventRepository eventRepository;
    private final EventChargeService eventChargeService;

    public EventAutoChargeJob(EventRepository eventRepository,
                              EventChargeService eventChargeService) {
        this.eventRepository = eventRepository;
        this.eventChargeService = eventChargeService;
    }

    /** Каждые 15 минут. */
    @Scheduled(cron = "0 */15 * * * *")
    public void chargeFinishedEvents() {
        List<Event> events = eventRepository
                .findByEventDateLessThanEqualAndStatusAndChargedAtIsNullAndChargeModeNot(
                        LocalDate.now(), "ACTIVE", "NONE");
        if (events.isEmpty()) return;

        for (Event e : events) {
            // Событие считается оконченным в указанное время
            if (!EventService.isEventPast(e)) continue;
            try {
                var result = eventChargeService.doCharge(e.getTeamId(), e.getId());
                log.info("Автоначисление по событию {} ({}): {}", e.getId(), e.getTitle(), result);
            } catch (Exception ex) {
                log.error("Ошибка автоначисления по событию {}: {}", e.getId(), ex.getMessage(), ex);
                // Ошибки данных (нет получателей, не настроено начисление и т.п.)
                // сами не рассосутся: помечаем событие обработанным, чтобы не
                // повторять попытку каждые 15 минут. При редактировании события
                // chargedAt сбрасывается — начисление можно переиграть.
                if (ex instanceof info.prorabka.abik.server.common.exception.BadRequestException
                        || ex instanceof info.prorabka.abik.server.common.exception.NotFoundException) {
                    try {
                        e.setChargedAt(java.time.OffsetDateTime.now());
                        eventRepository.save(e);
                    } catch (Exception saveEx) {
                        log.error("Не удалось пометить событие {} обработанным: {}",
                                e.getId(), saveEx.getMessage(), saveEx);
                    }
                }
            }
        }
    }
}
