package info.prorabka.abik.server.notification.service;

import info.prorabka.abik.server.notification.entity.NotificationType;
import info.prorabka.abik.server.notification.entity.UserDevice;

import java.util.List;
import java.util.Map;

/** Отправка push конкретному провайдеру по списку устройств. */
public interface ProviderPushSender {

    /** Имя провайдера: RUSTORE, FCM, HUAWEI. */
    String provider();

    /**
     * Отправить уведомление на устройства этого провайдера.
     * @return true, если хотя бы одно устройство получило уведомление успешно
     */
    boolean sendToDevices(List<UserDevice> devices, NotificationType type,
                          String title, String body, Map<String, Object> payload);
}