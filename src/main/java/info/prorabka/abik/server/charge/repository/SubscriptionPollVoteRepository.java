package info.prorabka.abik.server.charge.repository;

import info.prorabka.abik.server.charge.entity.SubscriptionPollVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPollVoteRepository extends JpaRepository<SubscriptionPollVote, Long> {
    List<SubscriptionPollVote> findByPollId(Long pollId);
    Optional<SubscriptionPollVote> findByPollIdAndUserId(Long pollId, Long userId);
}
