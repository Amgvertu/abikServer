package info.prorabka.abik.server.charge.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Планировщик генерации начислений.
 * Запускается каждый час — идемпотентность обеспечивается сервисом.
 *
 * Cron: "0 0 * * * *" — в 00 минут каждого часа.
 */
@Component
public class ChargeGenerationJob {

    private static final Logger log = LoggerFactory.getLogger(ChargeGenerationJob.class);

    private final ChargeService chargeService;

    public ChargeGenerationJob(ChargeService chargeService) {
        this.chargeService = chargeService;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void runHourly() {
        log.debug("Запуск планировщика генерации начислений");
        try {
            int created = chargeService.generateFromTemplates(LocalDate.now());
            if (created > 0) {
                log.info("Планировщик: создано {} начислений", created);
            } else {
                log.debug("Планировщик: новых начислений нет");
            }
        } catch (Exception e) {
            log.error("Ошибка планировщика генерации начислений", e);
        }
    }
}