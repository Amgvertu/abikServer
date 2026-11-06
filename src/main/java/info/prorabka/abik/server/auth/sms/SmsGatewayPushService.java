package info.prorabka.abik.server.auth.sms;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import info.prorabka.abik.server.notification.entity.UserDevice;
import info.prorabka.abik.server.notification.repository.UserDeviceRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** WAKE_UP для проекта SMSPush: сначала RuStore, затем FCM. */
@Service
public class SmsGatewayPushService {

    private static final Logger log = LoggerFactory.getLogger(SmsGatewayPushService.class);
    private static final String FCM_APP_NAME = "abik-sms-gateway";

    private final UserDeviceRepository deviceRepository;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${app.sms.gateway.rustore.enabled:false}")
    private boolean ruStoreEnabled;

    @Value("${app.sms.gateway.rustore.api-key:}")
    private String ruStoreApiKey;

    @Value("${app.sms.gateway.rustore.push-url:}")
    private String ruStorePushUrl;

    @Value("${app.sms.gateway.fcm.enabled:false}")
    private boolean fcmEnabled;

    @Value("${app.sms.gateway.fcm.service-account-path:}")
    private String fcmServiceAccountPath;

    private FirebaseApp gatewayFirebaseApp;

    public SmsGatewayPushService(UserDeviceRepository deviceRepository,
                                 ResourceLoader resourceLoader) {
        this.deviceRepository = deviceRepository;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    void initFirebase() {
        if (!fcmEnabled) return;
        try {
            gatewayFirebaseApp = FirebaseApp.getInstance(FCM_APP_NAME);
            return;
        } catch (IllegalStateException ignored) {
            // Приложение шлюза ещё не инициализировано.
        }
        try (InputStream input = resourceLoader.getResource(fcmServiceAccountPath).getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(input))
                    .build();
            gatewayFirebaseApp = FirebaseApp.initializeApp(options, FCM_APP_NAME);
            log.info("FCM SMS-шлюза инициализирован");
        } catch (Exception e) {
            log.error("Не удалось инициализировать FCM SMS-шлюза: {}", e.getMessage());
        }
    }

    public boolean sendWakeUp(Long gatewayUserId) {
        List<UserDevice> devices = deviceRepository.findByUserId(gatewayUserId);
        if (devices.isEmpty()) {
            log.warn("У SMS-шлюза Abik нет зарегистрированных push-токенов");
            return false;
        }

        List<UserDevice> ruStoreDevices = devices.stream()
                .filter(device -> "RUSTORE".equals(device.getProvider()))
                .toList();
        for (UserDevice device : ruStoreDevices) {
            if (sendRuStoreWakeUp(device)) return true;
        }

        List<UserDevice> fcmDevices = devices.stream()
                .filter(device -> "FCM".equals(device.getProvider()))
                .toList();
        for (UserDevice device : fcmDevices) {
            if (sendFcmWakeUp(device)) return true;
        }

        log.warn("Не удалось отправить WAKE_UP шлюзу Abik через RuStore и FCM");
        return false;
    }

    private boolean sendRuStoreWakeUp(UserDevice device) {
        if (!ruStoreEnabled || ruStoreApiKey.isBlank() || ruStorePushUrl.isBlank()) return false;
        try {
            Map<String, Object> payload = Map.of(
                    "message", Map.of(
                            "token", device.getToken(),
                            "data", Map.of("type", "WAKE_UP", "source", "ABIK")
                    )
            );
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ruStorePushUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + ruStoreApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 == 2) {
                log.info("WAKE_UP шлюза Abik отправлен через RuStore");
                return true;
            }
            log.warn("RuStore WAKE_UP шлюза Abik: HTTP {}", response.statusCode());
        } catch (Exception e) {
            log.warn("Ошибка RuStore WAKE_UP шлюза Abik: {}", e.getMessage());
        }
        return false;
    }

    private boolean sendFcmWakeUp(UserDevice device) {
        if (!fcmEnabled || gatewayFirebaseApp == null) return false;
        Message message = Message.builder()
                .setToken(device.getToken())
                .putData("type", "WAKE_UP")
                .putData("source", "ABIK")
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build())
                .build();
        try {
            FirebaseMessaging.getInstance(gatewayFirebaseApp).send(message);
            log.info("WAKE_UP шлюза Abik отправлен через FCM");
            return true;
        } catch (FirebaseMessagingException e) {
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                deviceRepository.deleteByToken(device.getToken());
            }
            log.warn("FCM WAKE_UP шлюза Abik: {}", e.getMessage());
            return false;
        }
    }
}
