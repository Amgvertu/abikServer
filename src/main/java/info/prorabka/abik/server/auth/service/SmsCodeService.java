package info.prorabka.abik.server.auth.service;

import info.prorabka.abik.server.auth.entity.SmsCode;
import info.prorabka.abik.server.auth.entity.SmsPurpose;
import info.prorabka.abik.server.auth.repository.SmsCodeRepository;
import info.prorabka.abik.server.auth.sms.SmsSender;
import info.prorabka.abik.server.common.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Сервис генерации, сохранения и проверки SMS-кодов.
 */
@Service
public class SmsCodeService {

    private static final Logger log = LoggerFactory.getLogger(SmsCodeService.class);

    private static final int CODE_LENGTH = 4;
    private static final int TTL_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;

    private final SmsCodeRepository smsCodeRepository;
    private final SmsSender smsSender;
    private final SecureRandom random = new SecureRandom();

    public SmsCodeService(SmsCodeRepository smsCodeRepository, SmsSender smsSender) {
        this.smsCodeRepository = smsCodeRepository;
        this.smsSender = smsSender;
    }

    /**
     * Сгенерировать новый код, сохранить и «отправить» через SmsSender.
     */
    @Transactional
    public void generateAndSend(String phone, SmsPurpose purpose) {
        String code = generateCode();

        SmsCode smsCode = SmsCode.builder()
                .phone(phone)
                .code(code)
                .purpose(purpose)
                .expiresAt(OffsetDateTime.now().plusMinutes(TTL_MINUTES))
                .used(false)
                .attempts((short) 0)
                .build();

        smsCodeRepository.save(smsCode);

        String text = "Ваш код подтверждения: " + code + ". Действует " + TTL_MINUTES + " мин.";
        smsSender.send(phone, text, purpose);

        log.debug("Сгенерирован SMS-код для {} ({})", phone, purpose);
    }

    /**
     * Проверить код. Если верный — пометить использованным.
     * Бросает BadRequestException при любой проблеме.
     */
    @Transactional
    public void verify(String phone, SmsPurpose purpose, String inputCode) {
        Optional<SmsCode> maybeCode = smsCodeRepository.findLatestActive(phone, purpose);

        if (maybeCode.isEmpty()) {
            throw new BadRequestException("Код не найден. Запросите новый.");
        }

        SmsCode smsCode = maybeCode.get();

        if (smsCode.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BadRequestException("Код истёк. Запросите новый.");
        }

        if (smsCode.getAttempts() >= MAX_ATTEMPTS) {
            throw new BadRequestException("Превышено число попыток. Запросите новый код.");
        }

        if (!smsCode.getCode().equals(inputCode)) {
            smsCode.setAttempts((short) (smsCode.getAttempts() + 1));
            smsCodeRepository.save(smsCode);
            throw new BadRequestException("Неверный код.");
        }

        smsCode.setUsed(true);
        smsCodeRepository.save(smsCode);
    }

    private String generateCode() {
        int value = random.nextInt((int) Math.pow(10, CODE_LENGTH));
        return String.format("%0" + CODE_LENGTH + "d", value);
    }
}