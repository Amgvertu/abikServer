package info.prorabka.abik.server.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Настройки OpenAPI / Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI abikOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Abik API")
                        .description("""
                                Backend REST API приложения «Абик» — учёт материальных взносов членов команды.

                                **Авторизация:** большинство эндпоинтов требует JWT access-токен.
                                Получите его через `POST /api/v1/auth/login` или `POST /api/v1/auth/register/confirm`,
                                затем нажмите кнопку **Authorize** и введите `Bearer <ваш_токен>`.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Info.Prorabka")
                                .url("https://varamy.online"))
                        .license(new License().name("Proprietary")))
                .servers(List.of(
                        // Относительный путь: Swagger сам подставит текущий IP (192.168.0.119:8083 или varamy.online)
                        new Server().url("/api").description("Текущий хост (прямое подключение / локальная сеть)"),

                        // Прод через Nginx
                        new Server().url("https://varamy.online/api/abik").description("Прод (через nginx)"),

                        // Локальный ПК
                        new Server().url("http://localhost:8080/api").description("Локальный ПК (Windows)")
                ))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Введите JWT access-токен (без префикса 'Bearer ')")
                        ));
    }
}