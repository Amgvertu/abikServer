package info.prorabka.abik.server.offline.repository;

import info.prorabka.abik.server.offline.entity.OfflineOperation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OfflineOperationRepository extends JpaRepository<OfflineOperation, Long> {
    Optional<OfflineOperation> findByUserIdAndOperationId(Long userId, UUID operationId);
}
