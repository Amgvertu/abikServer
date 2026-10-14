package info.prorabka.abik.server.geo.service;

import info.prorabka.abik.server.common.exception.NotFoundException;
import info.prorabka.abik.server.geo.entity.City;
import info.prorabka.abik.server.geo.entity.Region;
import info.prorabka.abik.server.geo.repository.CityRepository;
import info.prorabka.abik.server.geo.repository.RegionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис работы со справочниками регионов и городов.
 */
@Service
public class GeoService {

    private final RegionRepository regionRepository;
    private final CityRepository cityRepository;

    public GeoService(RegionRepository regionRepository, CityRepository cityRepository) {
        this.regionRepository = regionRepository;
        this.cityRepository = cityRepository;
    }

    public List<Region> getAllRegions() {
        return regionRepository.findAllByOrderByNameAsc();
    }

    public Region getRegion(Long id) {
        return regionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Регион не найден"));
    }

    public List<City> getCitiesByRegion(Long regionId) {
        return cityRepository.findByRegionIdOrderByNameAsc(regionId);
    }

    public List<City> searchCities(String query) {
        return cityRepository.findByNameIgnoreCaseContainingOrderByNameAsc(query);
    }

    public City getCity(Long id) {
        return cityRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Город не найден"));
    }
}