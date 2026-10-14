package info.prorabka.abik.server.auth.service;

import info.prorabka.abik.server.auth.entity.RefreshToken;
import info.prorabka.abik.server.auth.repository.RefreshTokenRepository;
import info.prorabka.abik.server.common.exception.UnauthorizedException;
import info.prorabka.abik.server.common.security.JwtProperties;
import info.prorabka.abik.server.common.security.TokenHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;

/**
 * Сервис управления refresh-токенами.
 * В БД храним только SHA-256 хеш, не сам токен.
 */
@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 48; // 48 байт = 64 символа в base64url

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHasher tokenHasher;
    private final JwtProperties jwtProperties;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               TokenHasher tokenHasher,
                               JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenHasher = tokenHasher;
        this.jwtProperties = jwtProperties;
    }

    /**
     * Создать новый refresh-токен для пользователя.
     * Возвращает сам токен (сырой) и момент истечения.
     */
    @Transactional
    public IssuedRefreshToken issue(Long userId) {
        String rawToken = generateRaw();
        String hash = tokenHasher.sha256(rawToken);
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(jwtProperties.getRefreshTtlDays());

        RefreshToken entity = RefreshToken.builder()
                .userId(userId)
                .tokenHash(hash)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        refreshTokenRepository.save(entity);
        return new IssuedRefreshToken(rawToken, expiresAt);
    }

    /**
     * Проверить refresh-токен и вернуть userId.
     * Бросает UnauthorizedException, если токен невалиден, истёк или отозван.
     */
    @Transactional
    public Long validateAndGetUserId(String rawToken) {
        String hash = tokenHasher.sha256(rawToken);
        RefreshToken token = refreshTokenRepository.findByTokenHashAndRevokedFalse(hash)
                .orElseThrow(() -> new UnauthorizedException("Refresh-токен недействителен"));

        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new UnauthorizedException("Refresh-токен истёк");
        }
        return token.getUserId();
    }

    /**
     * Отозвать конкретный refresh-токен (logout с одного устройства).
     */
    @Transactional
    public void revoke(String rawToken) {
        String hash = tokenHasher.sha256(rawToken);
        refreshTokenRepository.findByTokenHashAndRevokedFalse(hash)
                .ifPresent(t -> {
                    t.setRevoked(true);
                    t.setRevokedAt(OffsetDateTime.now());
                    refreshTokenRepository.save(t);
                });
    }

    /**
     * Отозвать все refresh-токены пользователя (logout со всех устройств).
     */
    @Transactional
    public int revokeAll(Long userId) {
        return refreshTokenRepository.revokeAllByUserId(userId, OffsetDateTime.now());
    }

    private String generateRaw() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Внутренний record для возврата пары (rawToken + expiresAt). */
    public record IssuedRefreshToken(String rawToken, OffsetDateTime expiresAt) {}
}