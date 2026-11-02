package info.prorabka.abik.server.charge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

/** Создание/обновление списка получателей. */
public record ChargeListRequest(
        @NotBlank @Size(max = 200) String name,
        Set<Long> memberIds
) {}
