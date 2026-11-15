package info.prorabka.abik.server.admin.service;

import info.prorabka.abik.server.common.exception.ForbiddenException;
import info.prorabka.abik.server.common.exception.NotFoundException;
import info.prorabka.abik.server.user.entity.User;
import info.prorabka.abik.server.user.entity.UserStatus;
import info.prorabka.abik.server.user.repository.UserRepository;
import org.springframework.stereotype.Service;

/** Единая проверка административных ролей для всех защищённых endpoint'ов. */
@Service
public class AdminAccessService {
    private final UserRepository users;

    public AdminAccessService(UserRepository users) { this.users = users; }

    public User requireSuperAdmin(Long userId) {
        User user = getActive(userId);
        if (!Boolean.TRUE.equals(user.getIsSuperAdmin())) {
            throw new ForbiddenException("Доступно только суперадмину");
        }
        return user;
    }

    public User requireAdManager(Long userId) {
        User user = getActive(userId);
        if (!Boolean.TRUE.equals(user.getIsSuperAdmin()) && !Boolean.TRUE.equals(user.getIsAdAdmin())) {
            throw new ForbiddenException("Недостаточно прав для управления рекламой");
        }
        return user;
    }

    public User getActive(Long userId) {
        User user = users.findById(userId).orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ForbiddenException("Аккаунт неактивен");
        }
        return user;
    }
}
