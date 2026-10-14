package info.prorabka.abik.server.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Настройки JWT, читаются из application.yml (блок app.jwt).
 */
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** Секрет для подписи JWT (HMAC-SHA256). Минимум 32 символа. */
    private String secret;

    /** Время жизни access-токена в минутах. */
    private long accessTtlMinutes = 30;

    /** Время жизни refresh-токена в днях. */
    private long refreshTtlDays = 60;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public long getAccessTtlMinutes() { return accessTtlMinutes; }
    public void setAccessTtlMinutes(long accessTtlMinutes) { this.accessTtlMinutes = accessTtlMinutes; }

    public long getRefreshTtlDays() { return refreshTtlDays; }
    public void setRefreshTtlDays(long refreshTtlDays) { this.refreshTtlDays = refreshTtlDays; }
}