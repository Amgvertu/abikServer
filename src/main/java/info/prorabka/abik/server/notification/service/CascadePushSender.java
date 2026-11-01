package info.prorabka.abik.server.notification.service;

import info.prorabka.abik.server.notification.entity.NotificationType;
import info.prorabka.abik.server.notification.entity.UserDevice;
import info.prorabka.abik.server.notification.repository.UserDeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Главный отправитель push: перебирает провайдеров в порядке
 * RuStore → FCM → Huawei и останавливается после первой успешной отправки.
 * Помечен @Primary — NotificationService получает именно его.
 */
@Primary
@Component
public class CascadePushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(CascadePushSender.class);

    private final UserDeviceRepository deviceRepository;
    private final RuStorePushSender ruStorePushSender;
    private final HuaweiPushSender huaweiPushSender;
    private final FcmPushSender fcmPushSender;

    public CascadePushSender(UserDeviceRepository deviceRepository,
                             RuStorePushSender ruStorePushSender,
                             HuaweiPushSender huaweiPushSender,
                             FcmPushSender fcmPushSender) {
        this.deviceRepository = deviceRepository;
        this.ruStorePushSender = ruStorePushSender;
        this.huaweiPushSender = huaweiPushSender;
        this.fcmPushSender = fcmPushSender;
    }

    @Override
    public boolean send(Long userId, String target, NotificationType type,
                        String title, String body, Map<String, Object> payload) {
        List<UserDevice> devices = deviceRepository.findByUserId(userId);
        if (devices.isEmpty()) {
            log.debug("[PUSH] у пользователя {} нет устройств", userId);
            return false;
        }

        // Порядок перебора: RuStore → FCM → Huawei
        List<ProviderPushSender> order = List.of(
                ruStorePushSender, fcmPushSenderAdapter(), huaweiPushSender);

        for (ProviderPushSender sender : order) {
            List<UserDevice> providerDevices = devices.stream()
                    .filter(d -> sender.provider().equals(d.getProvider()))
                    .toList();
            if (providerDevices.isEmpty()) continue;
            try {
                if (sender.sendToDevices(providerDevices, type, title, body, payload)) {
                    return true;
                }
            } catch (Exception e) {
                log.warn("[PUSH] провайдер {} упал: {}", sender.provider(), e.getMessage());
            }
        }
        log.info("[PUSH] ни один провайдер не доставил уведомление userId={}", userId);
        return false;
    }

    /** FCM-отправитель остался user-level PushSender — оборачиваем в provider-level. */
    private ProviderPushSender fcmPushSenderAdapter() {
        return new ProviderPushSender() {
            @Override
            public String provider() {
                return "FCM";
            }

            @Override
            public boolean sendToDevices(List<UserDevice> devices, NotificationType type,
                                         String title, String body, Map<String, Object> payload) {
                // FcmPushSender сам фильтрует FCM-токены (шаг 5),
                // поэтому просто делегируем ему по userId первого устройства
                if (devices.isEmpty()) return false;
                return fcmPushSender.send(devices.get(0).getUserId(), "abik-client",
                        type, title, body, payload);
            }
        };
    }
}