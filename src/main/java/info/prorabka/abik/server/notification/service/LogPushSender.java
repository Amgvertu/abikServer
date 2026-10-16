package info.prorabka.abik.server.notification.service;

import info.prorabka.abik.server.notification.entity.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Заглушка: пишет уведомление в лог.
 * Заменяется на реальный CompositePushSender, когда появятся device-токены.
 */
@Component
public class LogPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(LogPushSender.class);

    @Override
    public boolean send(Long userId, String target, NotificationType type,
                        String title, String body, Map<String, Object> payload) {
        log.info("[PUSH-MOCK] → userId={}, target={}, type={}, title=\"{}\", body=\"{}\", payload={}",
                userId, target, type, title, body, payload);
        return true;
    }
}