package info.prorabka.abik.server.charge.repository;

import info.prorabka.abik.server.charge.entity.SubscriptionPoll;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubscriptionPollRepository extends JpaRepository<SubscriptionPoll, Long> {
    Optional<SubscriptionPoll> findByTemplateIdAndPeriodYearAndPeriodMonth(
            Long templateId, Short periodYear, Short periodMonth);

    List<SubscriptionPoll> findByTeamIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByEndDateDesc(
            Long teamId, LocalDate startDate, LocalDate endDate);

    List<SubscriptionPoll> findByTeamIdAndProcessedAtIsNull(Long teamId);
}
