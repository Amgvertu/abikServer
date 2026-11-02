package info.prorabka.abik.server.charge.dto;

import java.util.Set;

/** Список получателей начислений. */
public record ChargeListResponse(
        Long id,
        String name,
        Set<Long> memberIds
) {}
