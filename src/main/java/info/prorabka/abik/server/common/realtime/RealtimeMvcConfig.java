package info.prorabka.abik.server.common.realtime;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RealtimeMvcConfig implements WebMvcConfigurer {
    private final RealtimeWebSocketHandler realtime;
    public RealtimeMvcConfig(RealtimeWebSocketHandler realtime) { this.realtime = realtime; }
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                    Object handler, Exception ex) {
                if (!"GET".equals(request.getMethod()) && response.getStatus() < 400 && ex == null) {
                    realtime.publishChange();
                }
            }
        }).addPathPatterns("/v1/**");
    }
}
