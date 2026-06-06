package tj.radolfa.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import tj.radolfa.domain.model.BannerSlot;

import java.time.Instant;

@Entity
@Table(name = "home_banners")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class HomeBannerEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "slot", nullable = false, length = 16)
    private BannerSlot slot;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "subtitle", length = 255)
    private String subtitle;

    @Column(name = "badge_text", length = 80)
    private String badgeText;

    @Column(name = "cta_label", length = 80)
    private String ctaLabel;

    @Column(name = "cta_url", length = 500)
    private String ctaUrl;

    @Column(name = "bg_color_hex", length = 9)
    private String bgColorHex;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
