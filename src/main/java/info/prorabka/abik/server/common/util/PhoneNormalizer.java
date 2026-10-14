package info.prorabka.abik.server.common.util;

import info.prorabka.abik.server.common.exception.BadRequestException;

/**
 * Нормализация телефонных номеров к формату E.164 (+7XXXXXXXXXX).
 */
public final class PhoneNormalizer {

    private PhoneNormalizer() {
        // утилитный класс — экземпляры не создаются
    }

    /**
     * Привести номер к формату +7XXXXXXXXXX (11 цифр, начинается с +7).
     * Примеры:
     *   "89991234567"      → "+79991234567"
     *   "+7 (999) 123-45-67" → "+79991234567"
     *   "79991234567"      → "+79991234567"
     */
    public static String normalize(String raw) {
        if (raw == null) {
            throw new BadRequestException("Телефон не может быть пустым");
        }

        // Оставляем только цифры
        String digits = raw.replaceAll("[^0-9]", "");

        if (digits.isEmpty()) {
            throw new BadRequestException("Телефон не содержит цифр");
        }

        // Приводим российские номера к +7XXXXXXXXXX
        if (digits.length() == 11 && digits.startsWith("8")) {
            digits = "7" + digits.substring(1);
        } else if (digits.length() == 10) {
            digits = "7" + digits;
        }

        if (digits.length() != 11 || !digits.startsWith("7")) {
            throw new BadRequestException(
                    "Некорректный номер телефона. Ожидается российский номер");
        }

        return "+" + digits;
    }
}
