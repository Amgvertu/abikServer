package info.prorabka.abik.server.geo.dto;

import info.prorabka.abik.server.geo.entity.Region;

public record RegionResponse(Long id, String name, String code) {

    public static RegionResponse from(Region r) {
        return new RegionResponse(r.getId(), r.getName(), r.getCode());
    }
}