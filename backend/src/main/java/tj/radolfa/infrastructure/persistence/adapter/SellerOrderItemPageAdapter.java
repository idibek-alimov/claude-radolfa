package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadSellerOrderItemsPort;
import tj.radolfa.application.readmodel.SellerOrderItemRow;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.infrastructure.persistence.repository.SellerOrderItemQueryRepository;
import tj.radolfa.infrastructure.persistence.repository.SellerOrderItemQueryRepository.SellerOrderItemProjection;

import java.util.List;

@Component
public class SellerOrderItemPageAdapter implements LoadSellerOrderItemsPort {

    private final SellerOrderItemQueryRepository queryRepo;

    public SellerOrderItemPageAdapter(SellerOrderItemQueryRepository queryRepo) {
        this.queryRepo = queryRepo;
    }

    @Override
    public PageResult<SellerOrderItemRow> findBySellerId(Long sellerId,
                                                         String search,
                                                         String sortBy,
                                                         String sortDir,
                                                         int page,
                                                         int size) {
        String safeSearch = (search == null) ? "" : search.trim();
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(direction, sortBy));

        Page<SellerOrderItemProjection> pageResult =
                queryRepo.findBySellerId(sellerId, safeSearch, pageable);

        List<SellerOrderItemRow> rows = pageResult.getContent().stream()
                .map(p -> new SellerOrderItemRow(
                        p.getOrderItemId(),
                        p.getOrderId(),
                        p.getOrderStatus() != null ? OrderStatus.valueOf(p.getOrderStatus()) : null,
                        p.getOrderCreatedAt(),
                        p.getProductName(),
                        p.getSkuCode(),
                        p.getQuantity(),
                        p.getPrice()))
                .toList();

        return new PageResult<>(rows, pageResult.getTotalElements(), page, size, !pageResult.hasNext());
    }
}
