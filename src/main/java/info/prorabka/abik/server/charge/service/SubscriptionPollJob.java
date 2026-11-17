package info.prorabka.abik.server.charge.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class SubscriptionPollJob {
    private static final Logger log = LoggerFactory.getLogger(SubscriptionPollJob.class);
    private final SubscriptionPollService pollService;

    public SubscriptionPollJob(SubscriptionPollService pollService) {
        this.pollService = pollService;
    }

    @Scheduled(cron = "0 7 * * * *")
    public void runHourly() {
        try {
            int created = pollService.process(LocalDate.now());
            if (created > 0) log.info("Создано начислений по опросам абонемента: {}", created);
        } catch (Exception e) {
            log.error("Ошибка планировщика опросов абонемента", e);
        }
    }
}
