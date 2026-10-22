package info.prorabka.abik.server.roster.repository;

import info.prorabka.abik.server.roster.entity.ChampionshipRoster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChampionshipRosterRepository extends JpaRepository<ChampionshipRoster, Long> {

    List<ChampionshipRoster> findByTeamIdAndSeasonOrderByAddedAtAsc(Long teamId, String season);

    List<ChampionshipRoster> findByTeamIdAndSeasonAndStatusOrderByAddedAtAsc(
            Long teamId, String season, String status);

    Optional<ChampionshipRoster> findByTeamIdAndSeasonAndUserId(Long teamId, String season, Long userId);

    @Query("SELECT DISTINCT r.season FROM ChampionshipRoster r WHERE r.teamId = :teamId ORDER BY r.season DESC")
    List<String> findDistinctSeasonsByTeamId(@Param("teamId") Long teamId);
}