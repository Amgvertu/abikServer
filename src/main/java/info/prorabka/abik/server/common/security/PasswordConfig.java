package info.prorabka.abik.server.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Настройка PasswordEncoder.
 * По ТЗ используется Argon2id.
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Параметры Argon2id:
        //   saltLength = 16, hashLength = 32,
        //   parallelism = 1, memory = 16384 KB (16 MB), iterations = 2
        // Это стандартный рекомендуемый набор для баланса безопасности и производительности.
        return new Argon2PasswordEncoder(16, 32, 1, 16384, 2);
    }
}