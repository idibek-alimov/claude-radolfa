package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;

import tj.radolfa.application.ports.out.LoadProductPort;
import tj.radolfa.application.ports.out.SaveProductPort;
import tj.radolfa.domain.model.Product;
import tj.radolfa.infrastructure.persistence.mappers.ProductMapper;
import tj.radolfa.infrastructure.persistence.repository.ProductRepository;

import java.util.List;
import java.util.Optional;

import tj.radolfa.application.ports.out.DeleteProductPort;

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
    public List<Product> loadTopSelling() {
        return repository.findByTopSellingTrue().stream()
                .map(mapper::toProduct)
                .toList();
    }

    @Override
    public Product save(Product product) {
        var entity = mapper.toEntity(product);
        var saved = repository.save(entity);
        return mapper.toProduct(saved);
    }

    @Override
    public void deleteByErpId(String erpId) {
        repository.findByErpId(erpId).ifPresent(repository::delete);
    }
}
