package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadSellerPort;
import tj.radolfa.application.ports.out.SaveSellerPort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.Seller;
import tj.radolfa.infrastructure.persistence.entity.SellerEntity;
import tj.radolfa.infrastructure.persistence.mappers.SellerMapper;
import tj.radolfa.infrastructure.persistence.repository.SellerRepository;

import java.util.List;
import java.util.Optional;

@Component
public class SellerJpaAdapter implements SaveSellerPort, LoadSellerPort {

    private final SellerRepository repository;
    private final SellerMapper mapper;

    public SellerJpaAdapter(SellerRepository repository, SellerMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Seller save(Seller seller) {
        SellerEntity entity = mapper.toEntity(seller);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<Seller> findByUserId(Long userId) {
        return repository.findByUserId(userId).map(mapper::toDomain);
    }

    @Override
    public Optional<Seller> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public PageResult<Seller> findAllPaged(int page, int size, String search) {
        var pageable = PageRequest.of(page - 1, size);
        Page<SellerEntity> result = repository.searchSellers(search, pageable);
        return toPageResult(result, page);
    }

    private PageResult<Seller> toPageResult(Page<SellerEntity> page, int pageNumber) {
        List<Seller> content = page.getContent().stream().map(mapper::toDomain).toList();
        return new PageResult<>(content, page.getTotalElements(), pageNumber, page.getSize(), page.isLast());
    }
}
