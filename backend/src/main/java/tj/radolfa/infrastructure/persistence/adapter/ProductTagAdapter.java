package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadProductTagPort;
import tj.radolfa.application.ports.out.SaveProductTagPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.ProductTag;
import tj.radolfa.infrastructure.persistence.entity.ProductTagEntity;
import tj.radolfa.infrastructure.persistence.repository.ProductTagRepository;

import java.util.List;
import java.util.Optional;

@Component
public class ProductTagAdapter implements LoadProductTagPort, SaveProductTagPort {

    private final ProductTagRepository tagRepo;

    public ProductTagAdapter(ProductTagRepository tagRepo) {
        this.tagRepo = tagRepo;
    }

    @Override
    public List<ProductTag> findAll() {
        return tagRepo.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<ProductTag> findById(Long id) {
        return tagRepo.findById(id).map(this::toDomain);
    }

    @Override
    public List<ProductTag> findAllByIds(List<Long> ids) {
        return tagRepo.findAllById(ids).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByName(String name) {
        return tagRepo.existsByName(name);
    }

    @Override
    public boolean existsByNameExcludingId(String name, Long excludeId) {
        return tagRepo.existsByNameAndIdNot(name, excludeId);
    }

    @Override
    public long countVariantsUsingTag(Long tagId) {
        return tagRepo.countVariantsUsingTag(tagId);
    }

    @Override
    public ProductTag save(String name, String colorHex) {
        ProductTagEntity entity = new ProductTagEntity();
        entity.setName(name);
        entity.setColorHex(colorHex);
        return toDomain(tagRepo.save(entity));
    }

    @Override
    public ProductTag update(Long id, String name, String colorHex) {
        ProductTagEntity entity = tagRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + id));
        entity.setName(name);
        entity.setColorHex(colorHex);
        return toDomain(tagRepo.save(entity));
    }

    @Override
    public void delete(Long id) {
        ProductTagEntity entity = tagRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found: " + id));
        tagRepo.delete(entity);
    }

    private ProductTag toDomain(ProductTagEntity entity) {
        return new ProductTag(entity.getId(), entity.getName(), entity.getColorHex());
    }
}
