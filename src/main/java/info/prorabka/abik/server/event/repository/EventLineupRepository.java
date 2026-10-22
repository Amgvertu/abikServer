package info.prorabka.abik.server.event.repository;

import info.prorabka.abik.server.event.entity.EventLineup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventLineupRepository extends JpaRepository<EventLineup, Long> {
    List<EventLineup> findByEventId(Long eventId);
    void deleteByEventId(Long eventId);
}
