package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadAdminProductPagePort;
import tj.radolfa.application.readmodel.AdminProductRow;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.ProductStatus;
import tj.radolfa.infrastructure.persistence.repository.AdminProductBaseQueryRepository;
import tj.radolfa.infrastructure.persistence.repository.AdminProductBaseQueryRepository.AdminProductRowProjection;

import java.util.List;

@Component
public class AdminProductPageAdapter implements LoadAdminProductPagePort {

    private final AdminProductBaseQueryRepository queryRepo;

    public AdminProductPageAdapter(AdminProductBaseQueryRepository queryRepo) {
        this.queryRepo = queryRepo;
    }

    @Override
    public PageResult<AdminProductRow> findAdminPage(ProductStatus status, String search,
                                                     int page, int size, Long sellerId) {
        String safeSearch = (search == null) ? "" : search.trim();
        String statusStr  = (status != null) ? status.name() : null;
        Pageable pageable = PageRequest.of(page - 1, size);

        Page<AdminProductRowProjection> pageResult =
                queryRepo.findAdminPage(statusStr, safeSearch, sellerId, pageable);

        List<AdminProductRow> rows = pageResult.getContent().stream()
                .map(p -> new AdminProductRow(
                        p.getProductBaseId(),
                        p.getExternalRef(),
                        p.getName(),
                        p.getStatus() != null ? ProductStatus.valueOf(p.getStatus()) : null,
                        p.getRejectionReason(),
                        p.getPrimaryImageUrl(),
                        p.getProductCode(),
                        p.getUpdatedAt()))
                .toList();

        return new PageResult<>(rows, pageResult.getTotalElements(), page, size, !pageResult.hasNext());
    }

    @Override
    public long countByStatus(ProductStatus status) {
        return queryRepo.countByStatus(status.name());
    }
}
