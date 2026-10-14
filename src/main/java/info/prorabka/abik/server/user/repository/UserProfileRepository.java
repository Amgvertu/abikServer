package info.prorabka.abik.server.user.repository;

import info.prorabka.abik.server.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    boolean existsByUserId(Long userId);
}
