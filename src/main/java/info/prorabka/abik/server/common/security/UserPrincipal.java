package info.prorabka.abik.server.common.security;

/**
 * Информация о текущем аутентифицированном пользователе.
 * Хранится в SecurityContext.
 */
public record UserPrincipal(Long userId) {}