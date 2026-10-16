package info.prorabka.abik.server.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Настройка CORS для веб-версии приложения.
 * Определяет, с каких домены браузеру разрешено обращаться к API.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // ─── Разрешённые источники (Origin) ───
        // Браузер пришлёт заголовок Origin, и если он в этом списке — запрос пройдёт.
        config.setAllowedOrigins(List.of(
                "https://abik.info",         // Прод-фронт
                "https://www.abik.info",     // Прод-фронт (с www)
                "http://localhost:3000",     // React dev-server (по умолчанию)
                "http://localhost:5173",     // Vite dev-server (по умолчанию)
                "http://localhost:8081"      // Другие варианты dev-фронта
        ));

        // ─── Разрешённые HTTP-методы ───
        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // ─── Разрешённые заголовки в запросе ───
        // Authorization — для JWT. Content-Type — для JSON.
        // Остальные — стандартные браузерные.
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With"
        ));

        // ─── Какие заголовки ответа доступны JS-коду фронта ───
        // Например, если фронт хочет читать access-token из заголовка ответа.
        config.setExposedHeaders(List.of("Authorization"));

        // ─── Разрешить передавать cookies и заголовок Authorization ───
        // ВАЖНО: при allowCredentials(true) нельзя ставить "*" в AllowedOrigins.
        // У нас список конкретных доменов — всё ок.
        config.setAllowCredentials(true);

        // ─── Кеш preflight-запроса (в секундах) ───
        // Браузер 1 час не будет переспрашивать OPTIONS для тех же путей.
        config.setMaxAge(3600L);

        // ─── Применить ко всем эндпоинтам ───
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}