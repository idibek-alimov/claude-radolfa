package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.out.DeleteProductPort;
import tj.radolfa.application.ports.out.LoadProductPort;
import tj.radolfa.application.ports.out.SaveProductPort;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.domain.model.Product;
import tj.radolfa.infrastructure.persistence.entity.ProductEntity;
import tj.radolfa.infrastructure.persistence.mappers.ProductMapper;
import tj.radolfa.infrastructure.persistence.repository.ProductRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Hexagonal adapter that bridges the application Out-Ports
 * ({@link LoadProductPort}, {@link SaveProductPort}, {@link DeleteProductPort})
 * to the
 * Spring Data {@link ProductRepository}.
 *
 * MapStruct handles all mapping; this class contains zero
 * hand-written transformation code.
 */
@Component
public class ProductRepositoryAdapter implements LoadProductPort, SaveProductPort, DeleteProductPort {

    private final ProductRepository repository;
    private final ProductMapper mapper;

    public ProductRepositoryAdapter(ProductRepository repository,
            ProductMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Product> load(String erpId) {
        return repository.findByErpId(erpId)
                .map(mapper::toProduct);
    }

    @Override
    public List<Product> loadAll() {
        return repository.findAll().stream()
                .map(mapper::toProduct)
                .toList();
    }

    @Override
    public PageResult<Product> loadPage(int page, int limit, String search) {
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("id").ascending());

        Specification<ProductEntity> spec = Specification.where(null);
        if (search != null && !search.isBlank()) {
            String term = search.trim().toLowerCase();
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), "%" + term + "%"),
                    cb.equal(cb.lower(root.get("erpId")), term)
            ));
        }

        Page<ProductEntity> jpaPage = repository.findAll(spec, pageable);

        List<Product> products = jpaPage.getContent().stream()
                .map(mapper::toProduct)
                .toList();

        return new PageResult<>(products, jpaPage.getTotalElements(), page, jpaPage.hasNext());
    }

    @Override
    public List<Product> loadTopSelling() {
        return repository.findByTopSellingTrue().stream()
                .map(mapper::toProduct)
                .toList();
    }

    @Override
    public Product save(Product product) {
        var entity = mapper.toEntity(product);

        // Map domain image URLs to entities and wire the bidirectional back-pointer
        var imageEntities = mapper.urlsToImages(product.getImages());
        entity.getImages().clear();
        imageEntities.forEach(img -> {
            img.setProduct(entity);
            entity.getImages().add(img);
        });

        var saved = repository.save(entity);
        return mapper.toProduct(saved);
    }

    @Override
    public void deleteByErpId(String erpId) {
        repository.findByErpId(erpId).ifPresent(entity -> {
            entity.setDeletedAt(Instant.now());
            repository.save(entity);
        });
    }
}
