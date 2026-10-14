package info.prorabka.abik.server.team.repository;

import info.prorabka.abik.server.team.entity.Team;
import info.prorabka.abik.server.team.entity.TeamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    List<Team> findByStatusOrderByNameAsc(TeamStatus status);

    List<Team> findAllByOrderByNameAsc();
}