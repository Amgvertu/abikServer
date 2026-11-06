package info.prorabka.abik.server.auth.sms;

import info.prorabka.abik.server.user.entity.User;
import info.prorabka.abik.server.user.repository.UserRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Доставка SMS-команд в отдельный STOMP-канал Android-шлюза Abik. */
@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "app.sms.gateway.enabled", havingValue = "true")
public class SmsGatewayService {

    private static final Logger log = LoggerFactory.getLogger(SmsGatewayService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final SimpUserRegistry userRegistry;
    private final SmsGatewayPushService pushService;
    private final UserRepository userRepository;
    private final Map<String, CompletableFuture<Boolean>> pendingRequests = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "abik-sms-gateway");
        thread.setDaemon(true);
        return thread;
    });

    @Value("${app.sms.gateway.enabled:false}")
    private boolean gatewayEnabled;

    @Value("${app.sms.gateway.phone:}")
    private String gatewayPhone;

    public SmsGatewayService(SimpMessagingTemplate messagingTemplate,
                             SimpUserRegistry userRegistry,
                             SmsGatewayPushService pushService,
                             UserRepository userRepository) {
        this.messagingTemplate = messagingTemplate;
        this.userRegistry = userRegistry;
        this.pushService = pushService;
        this.userRepository = userRepository;
    }

    public CompletableFuture<Boolean> sendSms(String phone, String code,
                                               String purpose, String message) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        if (!gatewayEnabled) {
            result.complete(false);
            return result;
        }
        User gateway = userRepository.findByPhone(gatewayPhone).orElse(null);
        if (gateway == null) {
            log.error("Техническая учётная запись SMS-шлюза не найдена");
            result.complete(false);
            return result;
        }

        String requestId = UUID.randomUUID().toString();
        pendingRequests.put(requestId, result);
        Long gatewayUserId = gateway.getId();
        scheduler.execute(() -> {
            try {
                if (!ensureGatewayConnected(gatewayUserId)) {
                    complete(requestId, false);
                    return;
                }
                SmsGatewayCommand command = new SmsGatewayCommand(
                        requestId, phone, code, purpose, "ABIK", message);
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(gatewayUserId), "/queue/sms-commands", command);
                log.info("SMS-команда отправлена шлюзу Abik: requestId={}, purpose={}",
                        requestId, purpose);
                scheduler.schedule(() -> complete(requestId, false), 15, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("Ошибка SMS-команды Abik: {}", e.getMessage(), e);
                complete(requestId, false);
            }
        });
        return result;
    }

    private boolean ensureGatewayConnected(Long gatewayUserId) {
        if (hasSession(gatewayUserId)) return true;

        log.info("WebSocket шлюза Abik не активен — отправка WAKE_UP");
        pushService.sendWakeUp(gatewayUserId);
        long deadline = System.currentTimeMillis() + 25_000L;
        while (System.currentTimeMillis() < deadline) {
            if (hasSession(gatewayUserId)) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
                return true;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        log.warn("Шлюз Abik не подключился за 25 секунд после WAKE_UP");
        return false;
    }

    private boolean hasSession(Long gatewayUserId) {
        SimpUser user = userRegistry.getUser(String.valueOf(gatewayUserId));
        return user != null && user.hasSessions();
    }

    public void handleSmsResponse(SmsGatewayResponse response) {
        if (response == null || response.requestId() == null) return;
        CompletableFuture<Boolean> future = pendingRequests.remove(response.requestId());
        if (future != null) {
            future.complete(response.success());
        } else {
            log.warn("Ответ шлюза Abik с неизвестным requestId={}", response.requestId());
        }
    }

    private void complete(String requestId, boolean success) {
        CompletableFuture<Boolean> future = pendingRequests.remove(requestId);
        if (future != null) future.complete(success);
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
        pendingRequests.values().forEach(future -> future.complete(false));
        pendingRequests.clear();
    }
}
