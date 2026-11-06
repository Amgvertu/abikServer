package info.prorabka.abik.server.auth.sms;

/** Команда Android-шлюзу. Поле message позволяет не смешивать шаблоны разных приложений. */
public record SmsGatewayCommand(
        String requestId,
        String phone,
        String code,
        String purpose,
        String source,
        String message
) {}
