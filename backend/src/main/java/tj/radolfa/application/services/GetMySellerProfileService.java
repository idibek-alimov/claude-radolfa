package tj.radolfa.application.services;

import org.springframework.stereotype.Service;
import tj.radolfa.application.ports.in.seller.GetMySellerProfileUseCase;
import tj.radolfa.application.ports.out.LoadSellerPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.Seller;

@Service
public class GetMySellerProfileService implements GetMySellerProfileUseCase {

    private final LoadSellerPort loadSellerPort;

    public GetMySellerProfileService(LoadSellerPort loadSellerPort) {
        this.loadSellerPort = loadSellerPort;
    }

    @Override
    public Seller execute(Long userId) {
        return loadSellerPort.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Seller profile not found for user " + userId));
    }
}
