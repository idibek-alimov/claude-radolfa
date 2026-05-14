package tj.radolfa.application.ports.in.discount;

import tj.radolfa.domain.model.DiscountOverlapRow;

import java.util.List;

public interface FindDiscountOverlapsUseCase {
    List<DiscountOverlapRow> execute();
}
