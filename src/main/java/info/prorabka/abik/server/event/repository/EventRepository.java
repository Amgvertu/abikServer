package info.prorabka.abik.server.event.repository;

import info.prorabka.abik.server.event.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByTeamIdAndEventDateBetweenOrderByEventDateAscTimeOfDayAsc(
            Long teamId, LocalDate from, LocalDate to);

    List<Event> findBySeriesId(Long seriesId);

    List<Event> findBySeriesIdAndEventDateGreaterThanEqual(Long seriesId, LocalDate from);
}