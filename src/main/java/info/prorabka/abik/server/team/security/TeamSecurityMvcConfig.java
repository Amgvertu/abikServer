package info.prorabka.abik.server.team.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class TeamSecurityMvcConfig implements WebMvcConfigurer {
    private final TeamBlockGuard teamBlockGuard;

    public TeamSecurityMvcConfig(TeamBlockGuard teamBlockGuard) { this.teamBlockGuard = teamBlockGuard; }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(teamBlockGuard).addPathPatterns("/v1/teams/{teamId}/**");
    }
}
