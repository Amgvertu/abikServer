package info.prorabka.abik.server.notification.service;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Отправка через RuStore Push API.
 * Нужны настройки из консоли RuStore (Push-уведомления → Проекты):
 *   app.rustore.enabled=true
 *   app.rustore.project-id=<ID проекта>
 *   app.rustore.service-token=<сервисный токен>
 *
 * ВАЖНО: notification не заполняем (иначе SDK покажет уведомление сам,
 * а наше onMessageReceived покажет второе) — title/body кладём в data.
 */
@Component
public class RuStorePushSender implements ProviderPushSender {

    private static final Logger log = LoggerFactory.getLogger(RuStorePushSender.class);

    private final UserDeviceRepository deviceRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${app.rustore.enabled:false}")
    private boolean enabled;

    @Value("${app.rustore.project-id:}")
    private String projectId;

    @Value("${app.rustore.service-token:}")
    private String serviceToken;

    @Value("${app.rustore.api-url:https://vpnservice.rustore.ru}")
    private String apiUrl;

    public RuStorePushSender(UserDeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Override
    public String provider() {
        return "RUSTORE";
    }

    @Override
    public boolean sendToDevices(List<UserDevice> devices, NotificationType type,
                                 String title, String body, Map<String, Object> payload) {
        if (!enabled || projectId.isBlank() || serviceToken.isBlank()) {
            if (!devices.isEmpty()) {
                log.info("[RUSTORE-MOCK] → devices={}, title=\"{}\"", devices.size(), title);
            }
            return false;
        }
        boolean anySuccess = false;
        for (UserDevice d : devices) {
            try {
                Map<String, String> data = new HashMap<>();
                data.put("title", title);
                data.put("body", body);
                data.put("type", type.name());
                if (payload != null) {
                    payload.forEach((k, v) -> data.put(k, String.valueOf(v)));
                }
                Map<String, Object> message = Map.of(
                        "message", Map.of(
                                "token", d.getToken(),
                                "data", data
                        )
                );
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl + "/v1/" + projectId + "/messages"))
                        .timeout(Duration.ofSeconds(15))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + serviceToken)
                        .POST(HttpRequest.BodyPublishers.ofString(
                                objectMapper.writeValueAsString(message)))
                        .build();
                HttpResponse<String> response = http.send(request,
                        HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() / 100 == 2) {
                    anySuccess = true;
                } else {
                    log.warn("[RUSTORE] ошибка отправки userId={}: HTTP {} {}",
                            d.getUserId(), response.statusCode(), response.body());
                    if (response.statusCode() == 404 || response.statusCode() == 400) {
                        deviceRepository.deleteByToken(d.getToken());
                    }
                }
            } catch (Exception e) {
                log.warn("[RUSTORE] ошибка отправки userId={}: {}", d.getUserId(), e.getMessage());
            }
        }
        return anySuccess;
    }
}