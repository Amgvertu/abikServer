package info.prorabka.abik.server.auth.sms;

/** Подтверждение Android-шлюза: постановка, оператор и итоговая доставка. */
public record SmsGatewayResponse(
        String requestId,
        boolean success,
        String errorMessage,
        String stage,
        String status,
        String errorCode
) {}
