package info.prorabka.abik.server.geo.repository;

import info.prorabka.abik.server.geo.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {

    List<City> findByRegionIdOrderByNameAsc(Long regionId);

    /** Поиск города по части имени (регистронезависимый), для автодополнения. */
    List<City> findByNameIgnoreCaseContainingOrderByNameAsc(String namePart);
}
