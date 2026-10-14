package info.prorabka.abik.server.auth.service;

import info.prorabka.abik.server.auth.dto.*;
import info.prorabka.abik.server.auth.entity.SmsPurpose;
import info.prorabka.abik.server.common.exception.BadRequestException;
import info.prorabka.abik.server.common.exception.ConflictException;
import info.prorabka.abik.server.common.exception.ForbiddenException;
import info.prorabka.abik.server.common.exception.UnauthorizedException;
import info.prorabka.abik.server.common.security.JwtService;
import info.prorabka.abik.server.common.util.PhoneNormalizer;
import info.prorabka.abik.server.user.dto.UserResponse;
import info.prorabka.abik.server.user.entity.User;
import info.prorabka.abik.server.user.entity.UserConsent;
import info.prorabka.abik.server.user.entity.UserStatus;
import info.prorabka.abik.server.user.repository.UserConsentRepository;
import info.prorabka.abik.server.user.repository.UserRepository;
import info.prorabka.abik.server.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Основная логика аутентификации: регистрация, логин, refresh, logout.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final UserConsentRepository userConsentRepository;
    private final UserService userService;
    private final SmsCodeService smsCodeService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       UserConsentRepository userConsentRepository,
                       UserService userService,
                       SmsCodeService smsCodeService,
                       RefreshTokenService refreshTokenService,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userConsentRepository = userConsentRepository;
        this.userService = userService;
        this.smsCodeService = smsCodeService;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    // ============================================================
    // Регистрация, шаг 1: запрос кода
    // ============================================================

    @Transactional
    public RegisterStartResponse registerStart(RegisterStartRequest req, String ipAddress) {
        // 1. Согласие на ПД — обязательное условие
        if (!Boolean.TRUE.equals(req.acceptedPd())) {
            throw new BadRequestException("Без согласия на обработку персональных данных регистрация невозможна");
        }

        // 2. Нормализуем телефон
        String phone = PhoneNormalizer.normalize(req.phone());

        // 3. Ищем существующего пользователя
        User user = userRepository.findByPhone(phone).orElse(null);

        if (user == null) {
            // 3a. Совсем новый пользователь — создаём со статусом PENDING
            user = userService.create(phone, passwordEncoder.encode(req.password()));
        } else {
            // 3b. Пользователь уже есть — проверяем статус
            switch (user.getStatus()) {
                case PENDING -> {
                    // Ещё не подтвердил — обновим пароль, отправим новый код
                    user.setPasswordHash(passwordEncoder.encode(req.password()));
                    userRepository.save(user);
                }
                case ACTIVE -> throw new ConflictException(
                        "Этот номер уже зарегистрирован. Воспользуйтесь входом или восстановлением пароля.");
                case BLOCKED -> throw new ForbiddenException("Аккаунт заблокирован");
                case DELETED -> throw new ConflictException(
                        "Этот номер ранее был использован. Обратитесь в поддержку.");
            }
        }

        // 4. Сохраняем согласие на ПД
        UserConsent consent = UserConsent.builder()
                .userId(user.getId())
                .policyVersion(req.policyVersion())
                .acceptedAt(OffsetDateTime.now())
                .ipAddress(ipAddress)
                .build();
        userConsentRepository.save(consent);

        // 5. Генерируем и «отправляем» код
        smsCodeService.generateAndSend(phone, SmsPurpose.REGISTRATION);

        log.info("Начата регистрация для {} (userId={})", phone, user.getId());

        return new RegisterStartResponse(phone,
                "Код подтверждения отправлен. Проверьте SMS.");
    }

    // ============================================================
    // Регистрация, шаг 2: подтверждение кода
    // ============================================================

    @Transactional
    public AuthResponse registerConfirm(RegisterConfirmRequest req) {
        String phone = PhoneNormalizer.normalize(req.phone());

        // 1. Проверяем код
        smsCodeService.verify(phone, SmsPurpose.REGISTRATION, req.code());

        // 2. Находим пользователя
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new BadRequestException("Пользователь не найден"));

        if (user.getStatus() != UserStatus.PENDING) {
            throw new BadRequestException("Регистрация уже подтверждена или невозможна");
        }

        // 3. Активируем
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        log.info("Регистрация подтверждена для {} (userId={})", phone, user.getId());

        // 4. Выдаём токены
        return buildAuthResponse(user);
    }

    // ============================================================
    // Логин
    // ============================================================

    @Transactional
    public AuthResponse login(LoginRequest req) {
        String phone = PhoneNormalizer.normalize(req.phone());

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new UnauthorizedException("Неверный телефон или пароль"));

        // Проверка статуса
        switch (user.getStatus()) {
            case ACTIVE -> { /* ok */ }
            case PENDING -> throw new ForbiddenException(
                    "Регистрация не завершена. Подтвердите номер SMS-кодом.");
            case BLOCKED -> throw new ForbiddenException("Аккаунт заблокирован");
            case DELETED -> throw new ForbiddenException("Аккаунт удалён");
        }

        // Проверка пароля
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            // Намеренно не уточняем, что именно неверно — телефон или пароль
            throw new UnauthorizedException("Неверный телефон или пароль");
        }

        log.info("Успешный вход: userId={}", user.getId());

        return buildAuthResponse(user);
    }

    // ============================================================
    // Refresh
    // ============================================================

    @Transactional
    public AuthResponse refresh(RefreshRequest req) {
        // 1. Проверяем refresh, получаем userId
        Long userId = refreshTokenService.validateAndGetUserId(req.refreshToken());

        // 2. Отзываем старый refresh (ротация)
        refreshTokenService.revoke(req.refreshToken());

        // 3. Проверяем, что пользователь ещё существует и активен
        User user = userService.getById(userId);
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ForbiddenException("Аккаунт неактивен");
        }

        // 4. Выдаём новую пару
        return buildAuthResponse(user);
    }

    // ============================================================
    // Logout
    // ============================================================

    @Transactional
    public void logout(LogoutRequest req) {
        // Отзываем refresh. Если токена не было — просто игнорируем.
        refreshTokenService.revoke(req.refreshToken());
    }

    // ============================================================
    // Helpers
    // ============================================================

    /**
     * Собрать AuthResponse: access + refresh + user.
     */
    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId());
        var issuedRefresh = refreshTokenService.issue(user.getId());

        TokenPair tokens = new TokenPair(
                accessToken,
                issuedRefresh.rawToken(),
                jwtService.getAccessExpiresAt(),
                issuedRefresh.expiresAt()
        );

        return new AuthResponse(tokens, UserResponse.from(user));
    }
}