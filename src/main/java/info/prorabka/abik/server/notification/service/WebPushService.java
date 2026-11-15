package info.prorabka.abik.server.notification.service;

import tools.jackson.databind.ObjectMapper;
import info.prorabka.abik.server.notification.dto.WebPushStatusResponse;
import info.prorabka.abik.server.notification.dto.WebPushSubscribeRequest;
import info.prorabka.abik.server.notification.entity.NotificationType;
import info.prorabka.abik.server.notification.entity.WebPushSubscription;
import info.prorabka.abik.server.notification.repository.WebPushSubscriptionRepository;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Регистрация браузерных подписок и доставка Web Push с VAPID. */
@Service
public class WebPushService {

    private static final Logger log = LoggerFactory.getLogger(WebPushService.class);

    private final WebPushSubscriptionRepository repository;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String subject;
    private final String encodedPublicKey;
    private final String encodedPrivateKey;

    private volatile PushService pushService;
    private volatile String browserPublicKey;

    public WebPushService(WebPushSubscriptionRepository repository,
                          ObjectMapper objectMapper,
                          @Value("${app.web-push.enabled:false}") boolean enabled,
                          @Value("${app.web-push.subject:mailto:admin@abik.info}") String subject,
                          @Value("${app.web-push.public-key:}") String encodedPublicKey,
                          @Value("${app.web-push.private-key:}") String encodedPrivateKey) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.subject = subject;
        this.encodedPublicKey = encodedPublicKey;
        this.encodedPrivateKey = encodedPrivateKey;
    }

    public WebPushStatusResponse status() {
        if (encodedPublicKey == null || encodedPublicKey.isBlank()) {
            return new WebPushStatusResponse(false, null);
        }
        try {
            return new WebPushStatusResponse(enabled, browserPublicKey());
        } catch (Exception e) {
            log.error("[WEB PUSH] некорректный публичный VAPID-ключ: {}", e.getMessage());
            return new WebPushStatusResponse(false, null);
        }
    }

    @Transactional
    public void subscribe(Long userId, WebPushSubscribeRequest request) {
        OffsetDateTime now = OffsetDateTime.now();
        WebPushSubscription subscription = repository.findByEndpoint(request.endpoint())
                .orElseGet(() -> WebPushSubscription.builder()
                        .endpoint(request.endpoint())
                        .createdAt(now)
                        .build());
        // Один браузерный endpoint всегда принадлежит только текущему владельцу.
        subscription.setUserId(userId);
        subscription.setP256dh(request.p256dh());
        subscription.setAuth(request.auth());
        subscription.setUserAgent(request.userAgent());
        subscription.setLastSeenAt(now);
        repository.save(subscription);
    }

    @Transactional
    public void unsubscribe(Long userId, String endpoint) {
        repository.findByEndpoint(endpoint).ifPresent(subscription -> {
            if (subscription.getUserId().equals(userId)) {
                repository.delete(subscription);
            }
        });
    }

    /** Отправляет на все браузеры пользователя независимо от результата нативных push. */
    @Transactional
    public boolean sendToUser(Long userId, NotificationType type, String title, String body,
                              Map<String, Object> payload) {
        if (!enabled) return false;
        List<WebPushSubscription> subscriptions = repository.findByUserId(userId);
        if (subscriptions.isEmpty()) return false;

        boolean delivered = false;
        for (WebPushSubscription subscription : subscriptions) {
            try {
                PushService sender = pushService();
                byte[] message = objectMapper.writeValueAsString(
                        message(type, title, body, payload)).getBytes(StandardCharsets.UTF_8);
                nl.martijndwars.webpush.Notification notification =
                        new nl.martijndwars.webpush.Notification(
                                subscription.getEndpoint(), subscription.getP256dh(),
                                subscription.getAuth(), message);
                HttpResponse response = sender.send(notification);
                int status = response.getStatusLine().getStatusCode();
                if (status >= 200 && status < 300) {
                    delivered = true;
                    subscription.setLastSeenAt(OffsetDateTime.now());
                    repository.save(subscription);
                } else {
                    log.warn("[WEB PUSH] HTTP {} userId={}, subscriptionId={}",
                            status, userId, subscription.getId());
                    if (status == 404 || status == 410) {
                        repository.delete(subscription);
                    }
                }
            } catch (Exception e) {
                log.warn("[WEB PUSH] ошибка доставки userId={}, subscriptionId={}: {}",
                        userId, subscription.getId(), e.getMessage());
            }
        }
        return delivered;
    }

    private Map<String, Object> message(NotificationType type, String title, String body,
                                        Map<String, Object> payload) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (payload != null) data.putAll(payload);
        data.put("type", type.name());
        data.put("url", data.containsKey("eventId") ? "/events/" + data.get("eventId") : "/notifications");

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("title", title);
        message.put("body", body == null ? "" : body);
        message.put("icon", "/icons/icon-192.png");
        message.put("badge", "/icons/badge-96.png");
        message.put("tag", "abik-" + type.name().toLowerCase() + "-" + data.getOrDefault("eventId", "general"));
        message.put("data", data);
        return message;
    }

    private PushService pushService() throws Exception {
        PushService current = pushService;
        if (current != null) return current;
        synchronized (this) {
            if (pushService == null) {
                PushService created = new PushService();
                created.setSubject(subject);
                created.setPublicKey(decodePublicKey());
                created.setPrivateKey(decodePrivateKey());
                pushService = created;
            }
            return pushService;
        }
    }

    private String browserPublicKey() throws Exception {
        String current = browserPublicKey;
        if (current != null) return current;
        ECPublicKey key = (ECPublicKey) decodePublicKey();
        byte[] encoded = new byte[65];
        encoded[0] = 4;
        writeUnsigned(key.getW().getAffineX().toByteArray(), encoded, 1);
        writeUnsigned(key.getW().getAffineY().toByteArray(), encoded, 33);
        browserPublicKey = Base64.getUrlEncoder().withoutPadding().encodeToString(encoded);
        return browserPublicKey;
    }

    private PublicKey decodePublicKey() throws Exception {
        byte[] bytes = Base64.getDecoder().decode(encodedPublicKey);
        return ecKeyFactory().generatePublic(new X509EncodedKeySpec(bytes));
    }

    private PrivateKey decodePrivateKey() throws Exception {
        byte[] bytes = Base64.getDecoder().decode(encodedPrivateKey);
        return ecKeyFactory().generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }

    private KeyFactory ecKeyFactory() throws Exception {
        return KeyFactory.getInstance("EC", new BouncyCastleProvider());
    }

    private static void writeUnsigned(byte[] source, byte[] target, int offset) {
        int sourceOffset = Math.max(0, source.length - 32);
        int length = Math.min(32, source.length);
        System.arraycopy(source, sourceOffset, target, offset + 32 - length, length);
    }
}
