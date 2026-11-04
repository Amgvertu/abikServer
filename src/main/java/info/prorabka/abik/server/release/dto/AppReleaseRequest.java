package info.prorabka.abik.server.release.dto;

import jakarta.validation.constraints.*;

public record AppReleaseRequest(@NotNull @Positive Integer versionCode,
                                @NotBlank @Size(max = 40) String versionName,
                                @NotBlank @Size(max = 10000) String releaseNotes,
                                @NotBlank @Size(max = 2000) String downloadUrl,
                                boolean mandatory) {}
