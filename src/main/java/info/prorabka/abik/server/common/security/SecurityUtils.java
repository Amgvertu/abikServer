package info.prorabka.abik.server.common.security;

import info.prorabka.abik.server.common.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Утилита для получения текущего пользователя из SecurityContext.
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    /** Получить userId текущего пользователя. Бросает 401, если не аутентифицирован. */
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Пользователь не аутентифицирован");
        }
        return principal.userId();
    }

    /** Текущий пользователь или null, если не аутентифицирован (без исключения). */
    public static Long getCurrentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return principal.userId();
    }
}
