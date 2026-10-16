package info.prorabka.abik.server.notification.service;

import info.prorabka.abik.server.notification.entity.NotificationType;

import java.util.Map;

/**
 * Абстракция отправки push-уведомлений.
 * Сейчас реализация — LogPushSender (пишет в лог).
 * Позже добавим CompositePushSender (RuStore → FCM).
 *
 * Параметр target — определяет, каким каналом отправлять:
 *   - "abik-client"      → мобильный клиент
 *   - "abik-sms-gateway" → самодельный SMS-шлюз
 * Зарезервировано на будущее.
 */
public interface PushSender {

    /**
     * Отправить push-уведомление.
     *
     * @param userId  получатель
     * @param target  целевая платформа ("abik-client" и т.д.)
     * @param type    тип события
     * @param title   заголовок
     * @param body    текст
     * @param payload дополнительные данные
     * @return true, если отправка успешна
     */
    boolean send(Long userId, String target, NotificationType type,
                 String title, String body, Map<String, Object> payload);
}