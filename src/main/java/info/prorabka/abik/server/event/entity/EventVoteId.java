package info.prorabka.abik.server.event.entity;

import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class EventVoteId implements Serializable {
    private Long eventId;
    private Long userId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventVoteId that)) return false;
        return Objects.equals(eventId, that.eventId) && Objects.equals(userId, that.userId);
    }
    @Override
    public int hashCode() { return Objects.hash(eventId, userId); }
}