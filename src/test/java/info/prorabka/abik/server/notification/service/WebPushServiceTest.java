package info.prorabka.abik.server.notification.service;

import tools.jackson.databind.ObjectMapper;
import info.prorabka.abik.server.notification.dto.WebPushSubscribeRequest;
import info.prorabka.abik.server.notification.entity.WebPushSubscription;
import info.prorabka.abik.server.notification.repository.WebPushSubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebPushServiceTest {

    @Mock
    private WebPushSubscriptionRepository repository;

    @Test
    void subscribeReassignsExistingEndpointToCurrentUser() {
        WebPushSubscription existing = WebPushSubscription.builder()
                .id(7L).userId(1L).endpoint("https://push.example/subscription")
                .p256dh("old").auth("old").build();
        when(repository.findByEndpoint(existing.getEndpoint())).thenReturn(Optional.of(existing));
        WebPushService service = service(false, "", "");
        WebPushSubscribeRequest request = new WebPushSubscribeRequest(
                existing.getEndpoint(), "new-p256dh", "new-auth", "test-browser");

        service.subscribe(42L, request);

        ArgumentCaptor<WebPushSubscription> captor = ArgumentCaptor.forClass(WebPushSubscription.class);
        verify(repository).save(captor.capture());
        assertEquals(42L, captor.getValue().getUserId());
        assertEquals("new-p256dh", captor.getValue().getP256dh());
        assertEquals("new-auth", captor.getValue().getAuth());
        assertNotNull(captor.getValue().getLastSeenAt());
    }

    @Test
    void unsubscribeDoesNotDeleteAnotherUsersSubscription() {
        WebPushSubscription existing = WebPushSubscription.builder()
                .id(8L).userId(1L).endpoint("https://push.example/foreign").build();
        when(repository.findByEndpoint(existing.getEndpoint())).thenReturn(Optional.of(existing));

        service(false, "", "").unsubscribe(42L, existing.getEndpoint());

        verify(repository, never()).delete(any());
    }

    @Test
    void statusReturnsRawBrowserKeyWithoutExposingPrivateKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(256);
        java.security.KeyPair pair = generator.generateKeyPair();
        String publicKey = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
        String privateKey = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());

        var status = service(true, publicKey, privateKey).status();

        assertTrue(status.enabled());
        assertNotNull(status.publicKey());
        byte[] raw = Base64.getUrlDecoder().decode(status.publicKey());
        assertEquals(65, raw.length);
        assertEquals(4, raw[0]);
        assertFalse(status.publicKey().equals(privateKey));
    }

    private WebPushService service(boolean enabled, String publicKey, String privateKey) {
        return new WebPushService(repository, new ObjectMapper(), enabled,
                "mailto:test@abik.info", publicKey, privateKey);
    }
}
