package info.prorabka.abik.server.charge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChargeSubgroupRequest(

        @NotBlank(message = "Название подгруппы обязательно")
        @Size(max = 150)
        String name
) {}