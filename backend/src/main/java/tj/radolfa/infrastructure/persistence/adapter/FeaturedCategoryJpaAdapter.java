package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadFeaturedCategoryPort;
import tj.radolfa.application.ports.out.SaveFeaturedCategoryPort;
import tj.radolfa.domain.model.FeaturedCategory;
import tj.radolfa.domain.model.PageResult;
import tj.radolfa.infrastructure.persistence.entity.FeaturedCategoryEntity;
import tj.radolfa.infrastructure.persistence.mappers.FeaturedCategoryMapper;
import tj.radolfa.infrastructure.persistence.repository.FeaturedCategoryRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class FeaturedCategoryJpaAdapter implements LoadFeaturedCategoryPort, SaveFeaturedCategoryPort {

    private static final Set<String> SORTABLE = Set.of("displayOrder", "createdAt");
    private static final String DEFAULT_SORT_FIELD = "displayOrder";

    private final FeaturedCategoryRepository repository;
    private final FeaturedCategoryMapper mapper;

    public FeaturedCategoryJpaAdapter(FeaturedCategoryRepository repository, FeaturedCategoryMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<FeaturedCategory> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<FeaturedCategory> findActiveOrdered() {
        return repository.findByActiveTrueOrderByDisplayOrderAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public PageResult<FeaturedCategory> findAllPaged(int page, int size, String search, String sort) {
        String normalizedSearch = (search == null) ? "" : search.trim();
        var pageable = PageRequest.of(page - 1, size, parseSort(sort));
        Page<FeaturedCategoryEntity> result = repository.search(normalizedSearch, pageable);
        return toPageResult(result, page);
    }

    @Override
    public FeaturedCategory save(FeaturedCategory featuredCategory) {
        return mapper.toDomain(repository.save(mapper.toEntity(featuredCategory)));
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.ASC, DEFAULT_SORT_FIELD);
        }
        String[] parts = sort.split(",", 2);
        String field = SORTABLE.contains(parts[0]) ? parts[0] : DEFAULT_SORT_FIELD;
        Sort.Direction direction = (parts.length > 1 && "desc".equalsIgnoreCase(parts[1]))
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, field);
    }

    private PageResult<FeaturedCategory> toPageResult(Page<FeaturedCategoryEntity> page, int pageNumber) {
        List<FeaturedCategory> content = page.getContent().stream().map(mapper::toDomain).toList();
        return new PageResult<>(content, page.getTotalElements(), pageNumber, page.getSize(), page.isLast());
    }
}
