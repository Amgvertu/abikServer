package info.prorabka.abik.server.payment.dto;

import info.prorabka.abik.server.payment.entity.Payment;
import java.time.LocalDate;

import java.time.OffsetDateTime;

/**
 * Представление платежа для клиента.
 * Подтягиваются ФИО участника, за которого внесён платёж, и того, кто внёс.
 */
public record PaymentResponse(
        Long id,
        Long teamId,
        Long userId,
        String userLastName,
        String userFirstName,
        Long amountKopecks,
        LocalDate paidAt,          // ← НОВОЕ
        String description,
        Long createdByUserId,
        String createdByLastName,
        String createdByFirstName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static PaymentResponse from(Payment p,
                                       String userLastName, String userFirstName,
                                       String byLastName, String byFirstName) {
        return new PaymentResponse(
                p.getId(), p.getTeamId(), p.getUserId(),
                userLastName, userFirstName,
                p.getAmountKopecks(),
                p.getPaidAt(),                    // ← НОВОЕ
                p.getDescription(),
                p.getCreatedByUserId(), byLastName, byFirstName,
                p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}