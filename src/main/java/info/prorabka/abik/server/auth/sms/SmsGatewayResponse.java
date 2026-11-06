package info.prorabka.abik.server.auth.sms;

/** Подтверждение от Android-шлюза о постановке SMS на отправку. */
public record SmsGatewayResponse(
        String requestId,
        boolean success,
        String errorMessage
) {}
