package info.prorabka.abik.server.geo.controller;

import info.prorabka.abik.server.geo.dto.CityResponse;
import info.prorabka.abik.server.geo.dto.RegionResponse;
import info.prorabka.abik.server.geo.service.GeoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Публичные справочники: регионы и города.
 * Доступны без авторизации (в SecurityConfig — открыть /api/v1/geo/**).
 */
@RestController
@RequestMapping("/api/v1/geo")
public class GeoController {

    private final GeoService geoService;

    public GeoController(GeoService geoService) {
        this.geoService = geoService;
    }

    /** Все регионы. */
    @GetMapping("/regions")
    public List<RegionResponse> regions() {
        return geoService.getAllRegions().stream()
                .map(RegionResponse::from)
                .toList();
    }

    /** Города в заданном регионе. */
    @GetMapping("/regions/{regionId}/cities")
    public List<CityResponse> citiesByRegion(@PathVariable Long regionId) {
        return geoService.getCitiesByRegion(regionId).stream()
                .map(CityResponse::from)
                .toList();
    }

    /** Поиск городов по части названия (для автодополнения). */
    @GetMapping("/cities/search")
    public List<CityResponse> searchCities(@RequestParam("q") String query) {
        return geoService.searchCities(query).stream()
                .map(CityResponse::from)
                .toList();
    }
}
