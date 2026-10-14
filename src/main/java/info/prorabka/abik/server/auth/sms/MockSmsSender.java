package info.prorabka.abik.server.auth.sms;

import info.prorabka.abik.server.auth.entity.SmsPurpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Заглушка отправки SMS — пишет сообщение в лог.
 * Активна, пока в конфиге app.sms.provider=mock (по умолчанию).
 */
@Component
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "mock", matchIfMissing = true)
public class MockSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(MockSmsSender.class);

    @Override
    public void send(String phone, String text, SmsPurpose purpose) {
        log.info("[SMS-MOCK] to={}, purpose={}, text=\"{}\"", phone, purpose, text);
    }
}
