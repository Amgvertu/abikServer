package info.prorabka.abik.server.auth.sms;

import info.prorabka.abik.server.auth.entity.SmsPurpose;

/**
 * Интерфейс отправки SMS.
 * Позволит в будущем заменить mock на реальный шлюз без изменения кода сервисов.
 */
public interface SmsSender {

    /**
     * Отправить SMS.
     *
     * @param phone   номер получателя в формате E.164
     * @param text    текст сообщения
     * @param purpose назначение (регистрация, восстановление пароля и т.д.)
     */
    void send(String phone, String text, SmsPurpose purpose);
}
