package info.prorabka.abik.server.event.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "event_votes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@IdClass(EventVoteId.class)
public class EventVote {

    @Id
    @Column(name = "event_id")
    private Long eventId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private VoteAnswer answer;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist @PreUpdate
    void onUpdate() { updatedAt = OffsetDateTime.now(); }
}