package info.prorabka.abik.server.auth.sms;

import info.prorabka.abik.server.common.security.JwtService;
import info.prorabka.abik.server.user.entity.UserStatus;
import info.prorabka.abik.server.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/** Отдельный STOMP endpoint, чтобы не менять существующий realtime /ws клиента Abik. */
@Configuration
@EnableWebSocketMessageBroker
@ConditionalOnProperty(name = "app.sms.gateway.enabled", havingValue = "true")
public class SmsGatewayStompConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Value("${app.sms.gateway.phone}")
    private String gatewayPhone;

    public SmsGatewayStompConfig(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-sms").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                        message, StompHeaderAccessor.class);
                if (accessor == null) return message;

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authorization = firstNativeHeader(accessor, "Authorization");
                    if (authorization == null || !authorization.startsWith("Bearer ")) {
                        throw new MessageDeliveryException("Missing gateway token");
                    }
                    Long userId;
                    try {
                        userId = jwtService.parseUserId(authorization.substring(7));
                    } catch (Exception e) {
                        throw new MessageDeliveryException("Invalid gateway token");
                    }
                    boolean allowed = userRepository.findById(userId)
                            .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                            .filter(user -> gatewayPhone.equals(user.getPhone()))
                            .isPresent();
                    if (!allowed) throw new MessageDeliveryException("Not an SMS gateway account");
                    accessor.setUser(new UsernamePasswordAuthenticationToken(
                            String.valueOf(userId), null, List.of()));
                } else if (accessor.getUser() == null) {
                    throw new MessageDeliveryException("Unauthenticated STOMP command");
                }
                return message;
            }
        });
    }

    private String firstNativeHeader(StompHeaderAccessor accessor, String name) {
        List<String> values = accessor.getNativeHeader(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }
}
