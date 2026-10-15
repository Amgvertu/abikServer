package info.prorabka.abik.server.charge.repository;

import info.prorabka.abik.server.charge.entity.ChargeSubgroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChargeSubgroupRepository extends JpaRepository<ChargeSubgroup, Long> {

    /** Подгруппы группы, отсортированные по названию. */
    List<ChargeSubgroup> findByGroupIdOrderByNameAsc(Long groupId);

    Optional<ChargeSubgroup> findByGroupIdAndName(Long groupId, String name);

    boolean existsByGroupIdAndName(Long groupId, String name);
}