package info.prorabka.abik.server.user.service;

import info.prorabka.abik.server.auth.entity.SmsPurpose;
import info.prorabka.abik.server.auth.service.RefreshTokenService;
import info.prorabka.abik.server.auth.service.SmsCodeService;
import info.prorabka.abik.server.common.exception.BadRequestException;
import info.prorabka.abik.server.common.exception.ConflictException;
import info.prorabka.abik.server.common.util.PhoneNormalizer;
import info.prorabka.abik.server.user.entity.PhoneHistory;
import info.prorabka.abik.server.user.entity.User;
import info.prorabka.abik.server.user.repository.PhoneHistoryRepository;
import info.prorabka.abik.server.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PhoneChangeService {

    private static final Logger log = LoggerFactory.getLogger(PhoneChangeService.class);
    private static final long TOKEN_TTL_MINUTES = 10;

    private final UserRepository userRepository;
    private final PhoneHistoryRepository historyRepo;
    private final SmsCodeService smsCodeService;
    private final RefreshTokenService refreshTokenService;

    /** Хранилище временных токенов смены. */
    private final Map<String, PendingChange> pending = new ConcurrentHashMap<>();

    private final SecureRandom random = new SecureRandom();

    public PhoneChangeService(UserRepository userRepository,
                              PhoneHistoryRepository historyRepo,
                              SmsCodeService smsCodeService,
                              RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.historyRepo = historyRepo;
        this.smsCodeService = smsCodeService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public void start(Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Пользователь не найден"));
        smsCodeService.generateAndSend(u.getPhone(), SmsPurpose.PHONE_CHANGE_OLD);
    }

    @Transactional
    public Map<String, String> verifyOld(Long userId, String code) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Пользователь не найден"));
        smsCodeService.verify(u.getPhone(), SmsPurpose.PHONE_CHANGE_OLD, code);

        String token = newToken();
        pending.put(token, new PendingChange(
                userId, OffsetDateTime.now().plusMinutes(TOKEN_TTL_MINUTES), null));
        return Map.of("changeToken", token);
    }

    @Transactional
    public void sendNew(Long userId, String changeToken, String newPhoneRaw) {
        PendingChange pc = requireToken(changeToken, userId);
        String newPhone = PhoneNormalizer.normalize(newPhoneRaw);
        User existing = userRepository.findByPhone(newPhone).orElse(null);
        if (existing != null && !existing.getId().equals(userId)) {
            throw new ConflictException("Этот номер уже занят");
        }
        if (newPhone.equals(userRepository.getReferenceById(userId).getPhone())) {
            throw new BadRequestException("Новый номер совпадает со старым");
        }
        pc.newPhone = newPhone;
        smsCodeService.generateAndSend(newPhone, SmsPurpose.PHONE_CHANGE_NEW);
    }

    @Transactional
    public void confirm(Long userId, String changeToken, String code) {
        PendingChange pc = requireToken(changeToken, userId);
        if (pc.newPhone == null) {
            throw new BadRequestException("Сначала запросите SMS на новый номер");
        }
        smsCodeService.verify(pc.newPhone, SmsPurpose.PHONE_CHANGE_NEW, code);

        User u = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Пользователь не найден"));
        String oldPhone = u.getPhone();
        u.setPhone(pc.newPhone);
        userRepository.save(u);

        historyRepo.save(PhoneHistory.builder()
                .userId(userId).oldPhone(oldPhone).newPhone(pc.newPhone).build());

        refreshTokenService.revokeAll(userId);
        pending.remove(changeToken);
        log.info("Номер изменён: userId={}", userId);
    }

    private PendingChange requireToken(String token, Long userId) {
        PendingChange pc = pending.get(token);
        if (pc == null || !pc.userId.equals(userId)) {
            throw new BadRequestException("Недействительный токен смены");
        }
        if (pc.expiresAt.isBefore(OffsetDateTime.now())) {
            pending.remove(token);
            throw new BadRequestException("Токен истёк");
        }
        return pc;
    }

    private String newToken() {
        byte[] b = new byte[32];
        random.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static class PendingChange {
        final Long userId;
        final OffsetDateTime expiresAt;
        String newPhone;
        PendingChange(Long userId, OffsetDateTime expiresAt, String newPhone) {
            this.userId = userId; this.expiresAt = expiresAt; this.newPhone = newPhone;
        }
    }
}