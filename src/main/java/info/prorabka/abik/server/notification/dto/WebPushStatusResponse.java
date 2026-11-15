package info.prorabka.abik.server.notification.dto;

/** Публичный VAPID-ключ в base64url — формат applicationServerKey браузера. */
public record WebPushStatusResponse(boolean enabled, String publicKey) {
}
