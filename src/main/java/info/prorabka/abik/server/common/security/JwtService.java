package info.prorabka.abik.server.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Date;

/**
 * Сервис для работы с JWT access-токенами.
 * Только access. Refresh-токены — отдельный сервис (RefreshTokenService).
 */
@Service
public class JwtService {

    private final JwtProperties props;
    private final SecretKey key;

    public JwtService(JwtProperties props) {
        this.props = props;
        byte[] bytes = props.getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret слишком короткий: нужно минимум 32 байта (256 бит)");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    /** Создать access-токен для пользователя. */
    public String generateAccessToken(Long userId) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime exp = now.plusMinutes(props.getAccessTtlMinutes());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", "access")
                .issuedAt(Date.from(now.toInstant()))
                .expiration(Date.from(exp.toInstant()))
                .signWith(key)
                .compact();
    }

    /** Сколько access-токен живёт — возвращаем момент истечения. */
    public OffsetDateTime getAccessExpiresAt() {
        return OffsetDateTime.now().plusMinutes(props.getAccessTtlMinutes());
    }

    /**
     * Разобрать токен и вернуть userId.
     * Бросает исключение, если токен невалиден или просрочен.
     */
    public Long parseUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.parseLong(claims.getSubject());
    }

    /** Проверить токен без выбрасывания исключения. */
    public boolean isValid(String token) {
        try {
            parseUserId(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}