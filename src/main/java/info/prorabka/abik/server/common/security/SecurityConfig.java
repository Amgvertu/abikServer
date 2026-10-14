package info.prorabka.abik.server.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Базовая конфигурация безопасности.
 * Пока открываем только /api/v1/health и Swagger, всё остальное — закрыто.
 * Аутентификацию по JWT добавим в модуле auth.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Отключаем CSRF — наш API stateless, работает по токенам
                .csrf(csrf -> csrf.disable())
                // Не создаём HTTP-сессии — только JWT
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Правила доступа
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/health/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                // Отключаем дефолтную форму логина и Basic-аутентификацию
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        // ⚠️ Ключевое изменение: возвращаем результат build(), а не сам http
        return http.build();
    }
}
