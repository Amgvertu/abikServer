package info.prorabka.abik.server.charge.repository;

import info.prorabka.abik.server.charge.entity.ChargeGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChargeGroupRepository extends JpaRepository<ChargeGroup, Long> {

    /** Группы команды, отсортированные по названию. */
    List<ChargeGroup> findByTeamIdOrderByNameAsc(Long teamId);

    /** Проверка: есть ли группа с таким именем в команде (уникальность). */
    Optional<ChargeGroup> findByTeamIdAndName(Long teamId, String name);

    boolean existsByTeamIdAndName(Long teamId, String name);
}