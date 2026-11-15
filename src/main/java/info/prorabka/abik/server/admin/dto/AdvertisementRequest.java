package info.prorabka.abik.server.admin.dto;

import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record AdvertisementRequest(
        @Size(max = 100) String title,
        @Size(max = 1000) String body,
        @Size(max = 500) String imageUrl,
        @Size(max = 2000) String linkUrl,
        @Size(max = 100) String buttonText,
        String targetType,
        Long targetId,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        Boolean active
) {}
