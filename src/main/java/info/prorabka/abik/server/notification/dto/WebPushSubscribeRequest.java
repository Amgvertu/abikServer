package info.prorabka.abik.server.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Ключи PushSubscription из браузера; endpoint не является нативным device token. */
public record WebPushSubscribeRequest(
        @NotBlank @Size(max = 2048) String endpoint,
        @NotBlank @Size(max = 512) String p256dh,
        @NotBlank @Size(max = 255) String auth,
        @Size(max = 512) String userAgent
) {
}
