package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tj.radolfa.application.ports.in.home.ManageHomeBannerUseCase;
import tj.radolfa.domain.model.BannerSlot;

import java.time.Instant;

public record HomeBannerRequestDto(
        @NotNull BannerSlot slot,
        @NotBlank String title,
        String subtitle,
        String badgeText,
        String ctaLabel,
        String ctaUrl,
        String bgColorHex,
        Instant expiresAt,
        boolean active
) {
    public ManageHomeBannerUseCase.Command toCommand() {
        return new ManageHomeBannerUseCase.Command(
                slot, title, subtitle, badgeText, ctaLabel, ctaUrl, bgColorHex, expiresAt, active);
    }
}
