package info.prorabka.abik.server.event.repository;

import info.prorabka.abik.server.event.entity.EventSeries;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventSeriesRepository extends JpaRepository<EventSeries, Long> {}