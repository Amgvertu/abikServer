package info.prorabka.abik.server.user.service;

import info.prorabka.abik.server.common.exception.NotFoundException;
import info.prorabka.abik.server.user.entity.User;
import info.prorabka.abik.server.user.entity.UserStatus;
import info.prorabka.abik.server.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис для работы с пользователями.
 * Пока минимальный — расширим при разработке модуля user.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Создать нового пользователя. */
    @Transactional
    public User create(String phone, String passwordHash) {
        User user = User.builder()
                .phone(phone)
                .passwordHash(passwordHash)
                .status(UserStatus.PENDING)
                .build();
        return userRepository.save(user);
    }

    /** Найти по id, иначе NotFoundException. */
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }

    /** Найти по телефону, иначе NotFoundException. */
    public User getByPhone(String phone) {
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }

    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }

    /** Активировать — после подтверждения SMS. */
    @Transactional
    public void activate(Long userId) {
        User user = getById(userId);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }
}
