package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.LoadReviewTraitPort;
import tj.radolfa.application.ports.out.SaveReviewTraitPort;
import tj.radolfa.domain.exception.ResourceNotFoundException;
import tj.radolfa.domain.model.ReviewTrait;
import tj.radolfa.infrastructure.persistence.entity.CategoryEntity;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantEntity;
import tj.radolfa.infrastructure.persistence.entity.ReviewTraitEntity;
import tj.radolfa.infrastructure.persistence.mappers.ReviewTraitMapper;
import tj.radolfa.infrastructure.persistence.repository.ListingVariantRepository;
import tj.radolfa.infrastructure.persistence.repository.ReviewTraitRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ReviewTraitAdapter implements LoadReviewTraitPort, SaveReviewTraitPort {

    private final ReviewTraitRepository  reviewTraitRepository;
    private final ReviewTraitMapper      reviewTraitMapper;
    private final ListingVariantRepository listingVariantRepository;

    public ReviewTraitAdapter(ReviewTraitRepository reviewTraitRepository,
                              ReviewTraitMapper reviewTraitMapper,
                              ListingVariantRepository listingVariantRepository) {
        this.reviewTraitRepository   = reviewTraitRepository;
        this.reviewTraitMapper       = reviewTraitMapper;
        this.listingVariantRepository = listingVariantRepository;
    }

    @Override
    public Optional<ReviewTrait> findById(Long id) {
        return reviewTraitRepository.findById(id).map(reviewTraitMapper::toDomain);
    }

    @Override
    public Optional<ReviewTrait> findByKey(String key) {
        return reviewTraitRepository.findByTraitKey(key).map(reviewTraitMapper::toDomain);
    }

    @Override
    public List<ReviewTrait> findAll() {
        return reviewTraitRepository.findAll().stream()
                .map(reviewTraitMapper::toDomain)
                .toList();
    }

    @Override
    public List<ReviewTrait> findAllByIds(Collection<Long> ids) {
        return reviewTraitRepository.findAllById(ids).stream()
                .map(reviewTraitMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByKey(String key) {
        return reviewTraitRepository.existsByTraitKey(key);
    }

    @Override
    public List<ReviewTrait> findByVariantId(Long listingVariantId) {
        Optional<ListingVariantEntity> variantOpt = listingVariantRepository.findById(listingVariantId);
        if (variantOpt.isEmpty()) return List.of();

        ListingVariantEntity variant = variantOpt.get();
        CategoryEntity category = variant.getProductBase() != null
                ? variant.getProductBase().getCategory()
                : null;

        // Walk the parent chain and collect traits deduplicated by id.
        Map<Long, ReviewTraitEntity> byId = new LinkedHashMap<>();
        CategoryEntity current = category;
        while (current != null) {
            for (ReviewTraitEntity trait : current.getReviewTraits()) {
                byId.putIfAbsent(trait.getId(), trait);
            }
            current = current.getParent();
        }

        return new ArrayList<>(byId.values()).stream()
                .map(reviewTraitMapper::toDomain)
                .toList();
    }

    @Override
    public ReviewTrait save(ReviewTrait trait) {
        ReviewTraitEntity entity;
        if (trait.getId() == null) {
            entity = reviewTraitMapper.toEntity(trait);
        } else {
            entity = reviewTraitRepository.findById(trait.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("ReviewTrait not found: id=" + trait.getId()));
            entity.setLabelI18n(trait.getLabelI18n());
            entity.setInputType(trait.getInputType());
        }
        return reviewTraitMapper.toDomain(reviewTraitRepository.save(entity));
    }

    @Override
    public void deleteById(Long id) {
        if (!reviewTraitRepository.existsById(id)) {
            throw new ResourceNotFoundException("ReviewTrait not found: id=" + id);
        }
        reviewTraitRepository.deleteById(id);
    }
}
