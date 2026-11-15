package info.prorabka.abik.server.auth.service;

import info.prorabka.abik.server.admin.service.RegistrationLogService;
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
import java.util.UUID;

/** Сервис генерации, сохранения и проверки SMS-кодов. */
@Service
public class SmsCodeService {

    private static final Logger log = LoggerFactory.getLogger(SmsCodeService.class);
    private static final int CODE_LENGTH = 4;
    private static final int TTL_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;

    private final SmsCodeRepository smsCodeRepository;
    private final SmsSender smsSender;
    private final RegistrationLogService registrationLogs;
    private final SecureRandom random = new SecureRandom();

    public SmsCodeService(SmsCodeRepository smsCodeRepository, SmsSender smsSender,
                          RegistrationLogService registrationLogs) {
        this.smsCodeRepository = smsCodeRepository;
        this.smsSender = smsSender;
        this.registrationLogs = registrationLogs;
    }

    @Transactional
    public void generateAndSend(String phone, SmsPurpose purpose) {
        String code = generateCode();
        String requestId = UUID.randomUUID().toString();
        SmsCode smsCode = SmsCode.builder()
                .phone(phone)
                .code(code)
                .purpose(purpose)
                .requestId(requestId)
                .expiresAt(OffsetDateTime.now().plusMinutes(TTL_MINUTES))
                .used(false)
                .attempts((short) 0)
                .build();
        smsCodeRepository.save(smsCode);

        registrationLogs.record(requestId, phone, "CODE", "REQUESTED", purpose.name(), null, "SERVER");
        String text = "Abik\nКод подтверждения: " + code;
        try {
            smsSender.send(phone, text, purpose, requestId);
        } catch (RuntimeException e) {
            registrationLogs.record(requestId, phone, "SEND", "ERROR", "PROVIDER_ERROR", e.getMessage(), "SERVER");
            throw e;
        }
        log.debug("Сгенерирован SMS-код для {} ({})", phone, purpose);
    }

    @Transactional
    public void verify(String phone, SmsPurpose purpose, String inputCode) {
        Optional<SmsCode> maybeCode = smsCodeRepository.findLatestActive(phone, purpose);
        if (maybeCode.isEmpty()) {
            registrationLogs.record(null, phone, "VERIFY", "ERROR", "CODE_NOT_FOUND", null, "SERVER");
            throw new BadRequestException("Код не найден. Запросите новый.");
        }

        SmsCode smsCode = maybeCode.get();
        String requestId = smsCode.getRequestId();
        if (smsCode.getExpiresAt().isBefore(OffsetDateTime.now())) {
            registrationLogs.record(requestId, phone, "VERIFY", "ERROR", "CODE_EXPIRED", null, "SERVER");
            throw new BadRequestException("Код истёк. Запросите новый.");
        }
        if (smsCode.getAttempts() >= MAX_ATTEMPTS) {
            registrationLogs.record(requestId, phone, "VERIFY", "ERROR", "TOO_MANY_ATTEMPTS", null, "SERVER");
            throw new BadRequestException("Превышено число попыток. Запросите новый код.");
        }
        if (!smsCode.getCode().equals(inputCode)) {
            smsCode.setAttempts((short) (smsCode.getAttempts() + 1));
            smsCodeRepository.save(smsCode);
            registrationLogs.record(requestId, phone, "VERIFY", "ERROR", "INVALID_CODE", null, "SERVER");
            throw new BadRequestException("Неверный код.");
        }
        smsCode.setUsed(true);
        smsCodeRepository.save(smsCode);
        registrationLogs.record(requestId, phone, "VERIFY", "SUCCESS", purpose.name(), null, "SERVER");
    }

    private String generateCode() {
        int value = random.nextInt((int) Math.pow(10, CODE_LENGTH));
        return String.format("%0" + CODE_LENGTH + "d", value);
    }
}
