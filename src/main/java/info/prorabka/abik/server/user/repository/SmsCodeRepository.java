package info.prorabka.abik.server.auth.repository;

import info.prorabka.abik.server.auth.entity.SmsCode;
import info.prorabka.abik.server.auth.entity.SmsPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Репозиторий SMS-кодов.
 */
@Repository
public interface SmsCodeRepository extends JpaRepository<SmsCode, Long> {

    /**
     * Найти последний неиспользованный код для телефона с заданной целью.
     * "Последний" — с максимальным created_at.
     */
    @Query("""
            SELECT s FROM SmsCode s
            WHERE s.phone = :phone
              AND s.purpose = :purpose
              AND s.used = false
            ORDER BY s.createdAt DESC
            LIMIT 1
            """)
    Optional<SmsCode> findLatestActive(@Param("phone") String phone,
                                       @Param("purpose") SmsPurpose purpose);
}