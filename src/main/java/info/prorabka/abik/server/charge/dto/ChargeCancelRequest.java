package info.prorabka.abik.server.charge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Отмена начисления с указанием причины.
 */
public record ChargeCancelRequest(

        @NotBlank(message = "Укажите причину отмены")
        @Size(max = 500)
        String reason
) {}