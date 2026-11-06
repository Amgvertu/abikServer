package info.prorabka.abik.server.auth.sms;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/** Приём подтверждений отправки от Android-шлюза. */
@Controller
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "app.sms.gateway.enabled", havingValue = "true")
public class SmsGatewayStompController {

    private final SmsGatewayService gatewayService;

    public SmsGatewayStompController(SmsGatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    @MessageMapping("/sms-response")
    public void handleSmsResponse(SmsGatewayResponse response, Principal principal) {
        if (principal == null) return;
        gatewayService.handleSmsResponse(response);
    }
}
