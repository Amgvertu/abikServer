package info.prorabka.abik.server.event.repository;

import info.prorabka.abik.server.event.entity.EventSweater;
import info.prorabka.abik.server.event.entity.EventSweaterId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventSweaterRepository extends JpaRepository<EventSweater, EventSweaterId> {
    List<EventSweater> findByEventId(Long eventId);
    void deleteByEventId(Long eventId);
}