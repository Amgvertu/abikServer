package info.prorabka.abik.server.notification.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import info.prorabka.abik.server.notification.entity.NotificationType;
import info.prorabka.abik.server.notification.entity.UserDevice;
import info.prorabka.abik.server.notification.repository.UserDeviceRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Реальная отправка FCM. Если Firebase не сконфигурирован — фолбэк в лог.
 */
@Component
public class FcmPushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(FcmPushSender.class);

    private final UserDeviceRepository deviceRepository;
    private final ResourceLoader resourceLoader;

    @Value("${app.fcm.enabled:false}")
    private boolean enabled;

    @Value("${app.fcm.service-account-path:}")
    private String serviceAccountPath;

    private boolean initialized = false;

    public FcmPushSender(UserDeviceRepository deviceRepository, ResourceLoader resourceLoader) {
        this.deviceRepository = deviceRepository;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    void init() {
        if (!enabled) {
            log.info("FCM отключён (app.fcm.enabled=false) — push пойдут в лог");
            return;
        }
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                Resource res = resourceLoader.getResource(serviceAccountPath);
                try (InputStream in = res.getInputStream()) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(in))
                            .build();
                    FirebaseApp.initializeApp(options);
                }
            }
            initialized = true;
            log.info("FCM инициализирован");
        } catch (Exception e) {
            log.error("Не удалось инициализировать Firebase: {}", e.getMessage(), e);
        }
    }

    @Override
    public boolean send(Long userId, String target, NotificationType type,
                        String title, String body, Map<String, Object> payload) {
        List<UserDevice> devices = deviceRepository.findByUserId(userId);
        if (devices.isEmpty()) {
            log.debug("[FCM] у пользователя {} нет устройств", userId);
            return false;
        }

        if (!initialized) {
            log.info("[FCM-MOCK] → userId={}, devices={}, title=\"{}\"",
                    userId, devices.size(), title);
            return true;
        }

        boolean anySuccess = false;
        for (UserDevice d : devices) {
            try {
                Message.Builder mb = Message.builder()
                        .setToken(d.getToken())
                        .setNotification(Notification.builder()
                                .setTitle(title).setBody(body).build());
                if (payload != null && !payload.isEmpty()) {
                    mb.putAllData(payload.entrySet().stream()
                            .collect(java.util.stream.Collectors.toMap(
                                    Map.Entry::getKey,
                                    e -> String.valueOf(e.getValue()))));
                }

                FirebaseMessaging.getInstance().send(mb.build());
                anySuccess = true;
            } catch (FirebaseMessagingException e) {
                String code = e.getMessagingErrorCode() != null
                        ? e.getMessagingErrorCode().name() : "";
                if ("UNREGISTERED".equals(code) || "INVALID_ARGUMENT".equals(code)) {
                    log.info("[FCM] удаляю невалидный токен userId={}", userId);
                    deviceRepository.deleteByToken(d.getToken());
                } else {
                    log.warn("[FCM] ошибка отправки userId={}: {}", userId, e.getMessage());
                }
            }
        }
        return anySuccess;
    }
}