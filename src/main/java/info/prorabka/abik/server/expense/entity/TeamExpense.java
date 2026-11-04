package info.prorabka.abik.server.expense.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "team_expenses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TeamExpense {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "team_id", nullable = false) private Long teamId;
    @Column(nullable = false, length = 80) private String category;
    @Column(name = "amount_kopecks", nullable = false) private Long amountKopecks;
    @Column(name = "expense_date", nullable = false) private LocalDate expenseDate;
    @Column(columnDefinition = "TEXT") private String comment;
    @Column(name = "event_id") private Long eventId;
    @Column(name = "created_by", nullable = false) private Long createdBy;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @PrePersist void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
