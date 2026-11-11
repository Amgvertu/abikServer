package info.prorabka.abik.server.event.repository;

import info.prorabka.abik.server.event.entity.Event;
import info.prorabka.abik.server.event.entity.EventSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByTeamIdAndEventDateBetweenOrderByEventDateAscTimeOfDayAsc(
            Long teamId, LocalDate from, LocalDate to);

    List<Event> findBySeriesId(Long seriesId);

    List<Event> findByVoterListId(Long voterListId);

    List<Event> findByTeamIdAndStatusAndEventDateGreaterThanEqual(
            Long teamId, String status, LocalDate from);

    List<Event> findByVoterListIdAndStatusAndEventDateGreaterThanEqual(
            Long voterListId, String status, LocalDate from);

    List<Event> findBySeriesIdAndEventDateGreaterThanEqual(Long seriesId, LocalDate from);

    /** Активные события в диапазоне дат для персональных напоминаний. */
    List<Event> findByEventDateBetweenAndStatus(LocalDate from, LocalDate to, String status);

    /**
     * Завершившиеся события с настроенным, но ещё не выполненным начислением
     * (для автоначисления после окончания).
     */
    List<Event> findByEventDateLessThanEqualAndStatusAndChargedAtIsNullAndChargeModeNot(
            LocalDate eventDate, String status, String chargeMode);
}