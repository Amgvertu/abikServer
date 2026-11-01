package info.prorabka.abik.server.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.prorabka.abik.server.notification.entity.NotificationType;
import info.prorabka.abik.server.notification.entity.UserDevice;
import info.prorabka.abik.server.notification.repository.UserDeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Отправка через Huawei Push Kit API.
 * Нужны настройки из AppGallery Connect:
 *   app.huawei.enabled=true
 *   app.huawei.app-id=<App ID>
 *   app.huawei.app-secret=<App Secret>
 */
@Component
public class HuaweiPushSender implements ProviderPushSender {

    private static final Logger log = LoggerFactory.getLogger(HuaweiPushSender.class);

    private final UserDeviceRepository deviceRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${app.huawei.enabled:false}")
    private boolean enabled;

    @Value("${app.huawei.app-id:}")
    private String appId;

    @Value("${app.huawei.app-secret:}")
    private String appSecret;

    private volatile String cachedToken;
    private volatile Instant tokenExpiresAt = Instant.EPOCH;

    public HuaweiPushSender(UserDeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Override
    public String provider() {
        return "HUAWEI";
    }

    @Override
    public boolean sendToDevices(List<UserDevice> devices, NotificationType type,
                                 String title, String body, Map<String, Object> payload) {
        if (!enabled || appId.isBlank() || appSecret.isBlank()) {
            if (!devices.isEmpty()) {
                log.info("[HUAWEI-MOCK] → devices={}, title=\"{}\"", devices.size(), title);
            }
            return false;
        }
        String accessToken = obtainAccessToken();
        if (accessToken == null) return false;

        boolean anySuccess = false;
        for (UserDevice d : devices) {
            try {
                Map<String, Object> message = Map.of(
                        "validate_only", false,
                        "message", Map.of(
                                "android", Map.of(
                                        "notification", Map.of(
                                                "title", title,
                                                "body", body
                                        )
                                ),
                                "token", List.of(d.getToken())
                        )
                );
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://push-api.cloud.huawei.com/v1/"
                                + appId + "/messages:send"))
                        .timeout(Duration.ofSeconds(15))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + accessToken)
                        .POST(HttpRequest.BodyPublishers.ofString(
                                objectMapper.writeValueAsString(message)))
                        .build();
                HttpResponse<String> response = http.send(request,
                        HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() / 100 == 2) {
                    anySuccess = true;
                } else {
                    log.warn("[HUAWEI] ошибка отправки userId={}: HTTP {} {}",
                            d.getUserId(), response.statusCode(), response.body());
                }
            } catch (Exception e) {
                log.warn("[HUAWEI] ошибка отправки userId={}: {}", d.getUserId(), e.getMessage());
            }
        }
        return anySuccess;
    }

    /** OAuth-токен Huawei с кэшем до истечения. */
    private synchronized String obtainAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiresAt)) {
            return cachedToken;
        }
        try {
            String form = "grant_type=client_credentials"
                    + "&client_id=" + appId
                    + "&client_secret=" + appSecret;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth-login.cloud.huawei.com/oauth2/v3/token"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();
            HttpResponse<String> response = http.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.warn("[HUAWEI] не удалось получить access_token: HTTP {} {}",
                        response.statusCode(), response.body());
                return null;
            }
            JsonNode json = objectMapper.readTree(response.body());
            cachedToken = json.path("access_token").asText(null);
            long expiresIn = json.path("expires_in").asLong(3600);
            tokenExpiresAt = Instant.now().plusSeconds(expiresIn - 60);
            return cachedToken;
        } catch (Exception e) {
            log.warn("[HUAWEI] ошибка получения access_token: {}", e.getMessage());
            return null;
        }
    }
}