package tj.radolfa.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import tj.radolfa.application.ports.in.seller.CreateSellerUseCase;

public record CreateSellerRequestDto(
        @NotBlank String phone,
        @NotBlank String shopName,
        String logoUrl,
        String bio
) {
    public CreateSellerUseCase.Command toCommand() {
        return new CreateSellerUseCase.Command(phone, shopName, logoUrl, bio);
    }
}
