package info.prorabka.abik.server.notification.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Регистрация устройства для push.
 * provider — FCM (по умолчанию), RUSTORE, HUAWEI.
 */
public record DeviceRegisterRequest(
        @NotBlank String token,
        @NotBlank String platform,
        String provider
) {}