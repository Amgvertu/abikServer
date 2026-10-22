package info.prorabka.abik.server.event.repository;

import info.prorabka.abik.server.event.entity.EventVote;
import info.prorabka.abik.server.event.entity.EventVoteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventVoteRepository extends JpaRepository<EventVote, EventVoteId> {

    List<EventVote> findByEventId(Long eventId);

    Optional<EventVote> findByEventIdAndUserId(Long eventId, Long userId);

    long countByEventIdAndAnswer(Long eventId,
                                 info.prorabka.abik.server.event.entity.VoteAnswer answer);
}