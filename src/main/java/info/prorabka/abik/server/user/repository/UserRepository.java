package info.prorabka.abik.server.user.repository;

import info.prorabka.abik.server.user.entity.User;
import info.prorabka.abik.server.user.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Репозиторий пользователей.
 * Spring сам генерирует реализацию по имени методов.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** Найти пользователя по телефону (в формате E.164). */
    Optional<User> findByPhone(String phone);

    /** Проверить существование пользователя с таким телефоном. */
    boolean existsByPhone(String phone);

    /** Найти по телефону и статусу (например, только ACTIVE). */
    Optional<User> findByPhoneAndStatus(String phone, UserStatus status);
}
