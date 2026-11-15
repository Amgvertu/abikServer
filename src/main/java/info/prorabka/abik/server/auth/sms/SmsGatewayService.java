package info.prorabka.abik.server.auth.sms;

import info.prorabka.abik.server.admin.service.RegistrationLogService;
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

import java.util.Locale;
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
    private final RegistrationLogService registrationLogs;
    private final Map<String, CompletableFuture<Boolean>> pendingRequests = new ConcurrentHashMap<>();
    private final Map<String, RequestContext> requestContexts = new ConcurrentHashMap<>();
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
                             UserRepository userRepository,
                             RegistrationLogService registrationLogs) {
        this.messagingTemplate = messagingTemplate;
        this.userRegistry = userRegistry;
        this.pushService = pushService;
        this.userRepository = userRepository;
        this.registrationLogs = registrationLogs;
    }

    public CompletableFuture<Boolean> sendSms(String phone, String code,
                                               String purpose, String message) {
        return sendSms(phone, code, purpose, message, null);
    }

    public CompletableFuture<Boolean> sendSms(String phone, String code,
                                               String purpose, String message, String requestId) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        final String traceId = requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
        requestContexts.put(traceId, new RequestContext(phone, purpose));
        scheduler.schedule(() -> requestContexts.remove(traceId), 24, TimeUnit.HOURS);
        if (!gatewayEnabled) {
            record(traceId, "SEND", "ERROR", "GATEWAY_DISABLED", "SMS-шлюз отключён");
            result.complete(false);
            return result;
        }
        User gateway = userRepository.findByPhone(gatewayPhone).orElse(null);
        if (gateway == null) {
            log.error("Техническая учётная запись SMS-шлюза не найдена");
            record(traceId, "SEND", "ERROR", "GATEWAY_ACCOUNT_MISSING", "Техническая учётная запись не найдена");
            result.complete(false);
            return result;
        }

        pendingRequests.put(traceId, result);
        Long gatewayUserId = gateway.getId();
        scheduler.execute(() -> {
            try {
                if (!ensureGatewayConnected(gatewayUserId, traceId)) {
                    complete(traceId, false, "GATEWAY_OFFLINE", "Шлюз не подключился после WAKE_UP");
                    return;
                }
                SmsGatewayCommand command = new SmsGatewayCommand(traceId, phone, code, purpose, "ABIK", message);
                messagingTemplate.convertAndSendToUser(String.valueOf(gatewayUserId), "/queue/sms-commands", command);
                record(traceId, "GATEWAY", "SUCCESS", "COMMAND_SENT", null);
                log.info("SMS-команда отправлена шлюзу Abik: requestId={}, purpose={}", traceId, purpose);
                scheduler.schedule(() -> complete(traceId, false, "GATEWAY_TIMEOUT",
                        "Нет ответа Android-шлюза за 15 секунд"), 15, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.error("Ошибка SMS-команды Abik: {}", e.getMessage(), e);
                complete(traceId, false, "GATEWAY_ERROR", e.getMessage());
            }
        });
        return result;
    }

    private boolean ensureGatewayConnected(Long gatewayUserId, String requestId) {
        if (hasSession(gatewayUserId)) return true;

        log.info("WebSocket шлюза Abik не активен — отправка WAKE_UP");
        record(requestId, "WAKE_UP", "REQUESTED", "FCM_WAKE_UP", null);
        pushService.sendWakeUp(gatewayUserId);
        long deadline = System.currentTimeMillis() + 25_000L;
        while (System.currentTimeMillis() < deadline) {
            if (hasSession(gatewayUserId)) {
                record(requestId, "WAKE_UP", "SUCCESS", "GATEWAY_ONLINE", null);
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
        String requestId = response.requestId();
        String rawStatus = response.status() == null ? "" : response.status().trim().toUpperCase(Locale.ROOT);
        String status = switch (rawStatus) {
            case "DELIVERED" -> "DELIVERED";
            case "DELIVERY_FAILED", "FAILED" -> "DELIVERY_FAILED";
            case "OPERATOR_ACCEPTED", "SENT" -> "OPERATOR_ACCEPTED";
            default -> response.success() ? "OPERATOR_ACCEPTED" : "ERROR";
        };
        String stage = response.stage();
        if (stage == null || stage.isBlank()) {
            stage = ("DELIVERED".equals(status) || "DELIVERY_FAILED".equals(status)) ? "DELIVERY" : "OPERATOR";
        }
        String code = response.errorCode();
        if (code == null || code.isBlank()) code = response.success() ? status : "GATEWAY_ERROR";
        record(requestId, stage, status, code, response.errorMessage());

        CompletableFuture<Boolean> future = pendingRequests.remove(requestId);
        if (future != null) {
            future.complete(response.success() && !"DELIVERY_FAILED".equals(status));
        } else if (!requestContexts.containsKey(requestId)) {
            log.warn("Ответ шлюза Abik с неизвестным requestId={}", requestId);
        }
    }

    private void complete(String requestId, boolean success, String code, String details) {
        CompletableFuture<Boolean> future = pendingRequests.remove(requestId);
        if (future != null) {
            record(requestId, "SEND", success ? "SUCCESS" : "ERROR", code, details);
            future.complete(success);
        }
    }

    private void record(String requestId, String stage, String status, String code, String details) {
        RequestContext context = requestContexts.get(requestId);
        registrationLogs.record(requestId, context == null ? null : context.phone(), stage, status,
                code, details, "ANDROID_GATEWAY");
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
        pendingRequests.values().forEach(future -> future.complete(false));
        pendingRequests.clear();
        requestContexts.clear();
    }

    private record RequestContext(String phone, String purpose) {}
}
