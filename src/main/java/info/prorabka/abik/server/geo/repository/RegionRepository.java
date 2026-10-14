package info.prorabka.abik.server.geo.repository;

import info.prorabka.abik.server.geo.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {

    /** Все регионы, отсортированные по имени. */
    List<Region> findAllByOrderByNameAsc();

    Optional<Region> findByCode(String code);
}