package tj.radolfa.application.ports.in.discount;

import tj.radolfa.domain.model.DiscountSummary;

import java.util.List;

public interface FindCampaignsByProductUseCase {
    List<DiscountSummary> execute(Long productBaseId);
}
