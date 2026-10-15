package info.prorabka.abik.server.geo.controller;

import info.prorabka.abik.server.geo.dto.CityResponse;
import info.prorabka.abik.server.geo.dto.RegionResponse;
import info.prorabka.abik.server.geo.service.GeoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Публичные справочники: регионы и города.
 */
@RestController
@RequestMapping("/v1/geo")
@Tag(name = "Geo",
        description = "Справочники регионов и городов РФ. Доступно без авторизации — данные публичные.")
public class GeoController {

    private final GeoService geoService;

    public GeoController(GeoService geoService) {
        this.geoService = geoService;
    }

    @Operation(
            summary = "Список регионов",
            description = "Возвращает все регионы РФ, отсортированные по названию."
    )
    @ApiResponse(responseCode = "200", description = "Список регионов",
            content = @Content(schema = @Schema(implementation = RegionResponse.class)))
    @GetMapping("/regions")
    public List<RegionResponse> regions() {
        return geoService.getAllRegions().stream()
                .map(RegionResponse::from)
                .toList();
    }

    @Operation(
            summary = "Города в регионе",
            description = "Возвращает список городов заданного региона, отсортированный по названию."
    )
    @ApiResponse(responseCode = "200", description = "Список городов",
            content = @Content(schema = @Schema(implementation = CityResponse.class)))
    @ApiResponse(responseCode = "404", description = "Регион не найден")
    @GetMapping("/regions/{regionId}/cities")
    public List<CityResponse> citiesByRegion(
            @Parameter(description = "ID региона", example = "1")
            @PathVariable Long regionId) {
        return geoService.getCitiesByRegion(regionId).stream()
                .map(CityResponse::from)
                .toList();
    }

    @Operation(
            summary = "Поиск городов по части названия",
            description = """
                    Используется для автодополнения при вводе города.
                    Поиск регистронезависимый, ищет вхождение подстроки.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Найденные города",
            content = @Content(schema = @Schema(implementation = CityResponse.class)))
    @GetMapping("/cities/search")
    public List<CityResponse> searchCities(
            @Parameter(description = "Часть названия города", example = "Мос")
            @RequestParam("q") String query) {
        return geoService.searchCities(query).stream()
                .map(CityResponse::from)
                .toList();
    }
}