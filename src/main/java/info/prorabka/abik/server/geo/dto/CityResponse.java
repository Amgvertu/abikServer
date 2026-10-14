package info.prorabka.abik.server.geo.dto;

import info.prorabka.abik.server.geo.entity.City;

public record CityResponse(Long id, Long regionId, String name) {

    public static CityResponse from(City c) {
        return new CityResponse(c.getId(), c.getRegionId(), c.getName());
    }
}