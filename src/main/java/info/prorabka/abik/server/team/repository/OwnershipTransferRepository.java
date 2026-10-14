package info.prorabka.abik.server.team.repository;

import info.prorabka.abik.server.team.entity.OwnershipTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OwnershipTransferRepository extends JpaRepository<OwnershipTransfer, Long> {

    /** История передач владения командой, свежие сверху. */
    List<OwnershipTransfer> findByTeamIdOrderByTransferredAtDesc(Long teamId);
}