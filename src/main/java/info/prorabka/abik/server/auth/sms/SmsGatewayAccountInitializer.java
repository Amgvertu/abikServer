package info.prorabka.abik.server.auth.sms;

import info.prorabka.abik.server.user.entity.User;
import info.prorabka.abik.server.user.entity.UserStatus;
import info.prorabka.abik.server.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Создаёт техническую учётную запись, под которой Android-шлюз входит в Abik. */
@Component
@ConditionalOnProperty(name = "app.sms.gateway.enabled", havingValue = "true")
public class SmsGatewayAccountInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SmsGatewayAccountInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.sms.gateway.phone}")
    private String gatewayPhone;

    @Value("${app.sms.gateway.password}")
    private String gatewayPassword;

    public SmsGatewayAccountInitializer(UserRepository userRepository,
                                        PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.findByPhone(gatewayPhone).isPresent()) {
            log.info("Учётная запись SMS-шлюза уже существует");
            return;
        }
        User gateway = User.builder()
                .phone(gatewayPhone)
                .passwordHash(passwordEncoder.encode(gatewayPassword))
                .status(UserStatus.ACTIVE)
                .isSuperAdmin(false)
                .build();
        userRepository.save(gateway);
        log.info("Создана техническая учётная запись SMS-шлюза");
    }
}
