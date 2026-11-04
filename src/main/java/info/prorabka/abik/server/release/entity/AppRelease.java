package info.prorabka.abik.server.release.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "app_releases", uniqueConstraints = @UniqueConstraint(
        name = "uq_app_release", columnNames = {"platform", "version_code"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppRelease {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 20) private String platform;
    @Column(name = "version_code", nullable = false) private Integer versionCode;
    @Column(name = "version_name", nullable = false, length = 40) private String versionName;
    @Column(name = "release_notes", nullable = false, columnDefinition = "TEXT") private String releaseNotes;
    @Column(name = "download_url", nullable = false, columnDefinition = "TEXT") private String downloadUrl;
    @Column(nullable = false) private Boolean mandatory;
    @Column(name = "published_at", nullable = false) private OffsetDateTime publishedAt;
    @PrePersist void onCreate() { if (publishedAt == null) publishedAt = OffsetDateTime.now(); }
}
