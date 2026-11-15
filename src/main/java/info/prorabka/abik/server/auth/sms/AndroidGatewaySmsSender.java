package info.prorabka.abik.server.auth.sms;

import info.prorabka.abik.server.auth.entity.SmsPurpose;
import info.prorabka.abik.server.common.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Реальная отправка SMS через общий Android-шлюз. */
@Component
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "android-gateway")
public class AndroidGatewaySmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(AndroidGatewaySmsSender.class);
    private final SmsGatewayService gatewayService;

    public AndroidGatewaySmsSender(SmsGatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    @Override
    public void send(String phone, String text, SmsPurpose purpose) {
        send(phone, text, purpose, null);
    }

    @Override
    public void send(String phone, String text, SmsPurpose purpose, String requestId) {
        String code = extractCode(text);
        try {
            CompletableFuture<Boolean> future = gatewayService.sendSms(phone, code, purpose.name(), text, requestId);
            if (!future.get(50, TimeUnit.SECONDS)) {
                throw new BadRequestException("SMS-шлюз недоступен. Попробуйте ещё раз.");
            }
            log.info("SMS передана Android-шлюзу: phone={}, purpose={}, requestId={}", phone, purpose, requestId);
        } catch (BadRequestException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("Отправка SMS прервана. Попробуйте ещё раз.");
        } catch (Exception e) {
            log.error("Ошибка отправки SMS через Android-шлюз: {}", e.getMessage(), e);
            throw new BadRequestException("Не удалось отправить SMS. Попробуйте ещё раз.");
        }
    }

    private String extractCode(String text) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\b\\d{4,8}\\b").matcher(text);
        return matcher.find() ? matcher.group() : "";
    }
}
