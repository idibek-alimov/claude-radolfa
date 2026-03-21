package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadDiscountTypePort;
import tj.radolfa.application.ports.out.SaveDiscountTypePort;
import tj.radolfa.domain.model.DiscountType;
import tj.radolfa.infrastructure.persistence.mappers.DiscountTypeMapper;
import tj.radolfa.infrastructure.persistence.repository.DiscountRepository;
import tj.radolfa.infrastructure.persistence.repository.DiscountTypeRepository;

import java.util.List;
import java.util.Optional;

@Component
public class DiscountTypeAdapter implements LoadDiscountTypePort, SaveDiscountTypePort {

    private final DiscountTypeRepository typeRepository;
    private final DiscountRepository discountRepository;
    private final DiscountTypeMapper mapper;

    public DiscountTypeAdapter(DiscountTypeRepository typeRepository,
                               DiscountRepository discountRepository,
                               DiscountTypeMapper mapper) {
        this.typeRepository = typeRepository;
        this.discountRepository = discountRepository;
        this.mapper = mapper;
    }

    // ---- LoadDiscountTypePort ----

    @Override
    public List<DiscountType> findAll() {
        return typeRepository.findAllByOrderByRankAsc().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<DiscountType> findById(Long id) {
        return typeRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public long countDiscountsByTypeId(Long typeId) {
        return discountRepository.countByType_Id(typeId);
    }

    // ---- SaveDiscountTypePort ----

    @Override
    public DiscountType save(DiscountType type) {
        return mapper.toDomain(typeRepository.save(mapper.toEntity(type)));
    }

    @Override
    public void deleteById(Long id) {
        typeRepository.deleteById(id);
    }
}
