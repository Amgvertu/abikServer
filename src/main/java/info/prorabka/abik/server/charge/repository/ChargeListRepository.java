package info.prorabka.abik.server.charge.repository;

import info.prorabka.abik.server.charge.entity.ChargeList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChargeListRepository extends JpaRepository<ChargeList, Long> {

    List<ChargeList> findByTeamIdOrderByNameAsc(Long teamId);

    List<ChargeList> findByTeamId(Long teamId);
}
