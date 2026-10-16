package info.prorabka.abik.server.payment.repository;

import info.prorabka.abik.server.payment.entity.PaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentAllocationRepository extends JpaRepository<PaymentAllocation, Long> {

    /** Все аллокации конкретного платежа. */
    List<PaymentAllocation> findByPaymentId(Long paymentId);

    /** Все аллокации конкретного начисления (обратная связь). */
    List<PaymentAllocation> findByChargeId(Long chargeId);

    /** Удалить все аллокации платежа (при soft-delete). */
    void deleteByPaymentId(Long paymentId);

    /** Удалить все аллокации начисления (при отмене). */
    void deleteByChargeId(Long chargeId);
}