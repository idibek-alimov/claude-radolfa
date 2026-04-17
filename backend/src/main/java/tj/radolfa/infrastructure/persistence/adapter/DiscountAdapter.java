package tj.radolfa.infrastructure.persistence.adapter;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.DiscountFilter;
import tj.radolfa.application.ports.out.LoadBestActiveDiscountPort;
import tj.radolfa.application.ports.out.LoadDiscountPort;
import tj.radolfa.application.ports.out.SaveDiscountPort;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.infrastructure.persistence.entity.DiscountEntity;
import tj.radolfa.infrastructure.persistence.entity.DiscountTypeEntity;
import tj.radolfa.infrastructure.persistence.mappers.DiscountMapper;
import tj.radolfa.infrastructure.persistence.repository.DiscountRepository;
import tj.radolfa.infrastructure.persistence.repository.DiscountTypeRepository;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class DiscountAdapter implements LoadDiscountPort, SaveDiscountPort, LoadBestActiveDiscountPort {

    private final DiscountRepository repository;
    private final DiscountTypeRepository typeRepository;
    private final DiscountMapper mapper;

    public DiscountAdapter(DiscountRepository repository,
                           DiscountTypeRepository typeRepository,
                           DiscountMapper mapper) {
        this.repository = repository;
        this.typeRepository = typeRepository;
        this.mapper = mapper;
    }

    // ---- LoadDiscountPort ----

    @Override
    public Optional<Discount> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Discount> findActiveByItemCode(String itemCode) {
        return repository.findActiveDiscountsByItemCode(itemCode).stream()
                .map(row -> mapper.toDomain((DiscountEntity) row[0]))
                .toList();
    }

    @Override
    public List<Discount> findActiveByItemCodes(Collection<String> itemCodes) {
        if (itemCodes.isEmpty()) return List.of();
        Map<Long, Discount> deduped = new LinkedHashMap<>();
        for (Object[] row : repository.findActiveDiscountsByItemCodes(itemCodes)) {
            DiscountEntity entity = (DiscountEntity) row[0];
            deduped.putIfAbsent(entity.getId(), mapper.toDomain(entity));
        }
        return List.copyOf(deduped.values());
    }

    @Override
    public Page<Discount> findAll(DiscountFilter filter, Pageable pageable) {
        Specification<DiscountEntity> spec = buildSpec(filter);
        return repository.findAll(spec, pageable).map(mapper::toDomain);
    }

    // ---- LoadBestActiveDiscountPort ----

    @Override
    public Optional<Discount> findBestActiveForItemCode(String itemCode) {
        return repository.findActiveDiscountsByItemCode(itemCode).stream()
                .findFirst() // already ordered by type.rank ASC — first is highest priority
                .map(row -> mapper.toDomain((DiscountEntity) row[0]));
    }

    // ---- SaveDiscountPort ----

    @Override
    public Discount save(Discount discount) {
        DiscountTypeEntity typeEntity = typeRepository.findById(discount.type().id())
                .orElseThrow(() -> new IllegalStateException(
                        "Discount type not found: " + discount.type().id()));

        DiscountEntity entity;
        if (discount.id() != null) {
            entity = repository.findById(discount.id())
                    .orElseThrow(() -> new IllegalStateException(
                            "Discount not found: " + discount.id()));
            entity.setType(typeEntity);
            entity.setDiscountValue(discount.discountValue());
            entity.setValidFrom(discount.validFrom());
            entity.setValidUpto(discount.validUpto());
            entity.setDisabled(discount.disabled());
            entity.setTitle(discount.title());
            entity.setColorHex(discount.colorHex());
            entity.getItemCodes().clear();
            entity.getItemCodes().addAll(discount.itemCodes());
        } else {
            entity = mapper.toEntity(discount);
            entity.setType(typeEntity);
            entity.getItemCodes().addAll(discount.itemCodes());
        }

        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    // ---- Specification builder ----

    private Specification<DiscountEntity> buildSpec(DiscountFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.typeId() != null) {
                predicates.add(cb.equal(root.get("type").get("id"), filter.typeId()));
            }

            if (filter.from() != null) {
                Instant fromInstant = filter.from().atStartOfDay(ZoneOffset.UTC).toInstant();
                predicates.add(cb.greaterThanOrEqualTo(root.get("validFrom"), fromInstant));
            }

            if (filter.to() != null) {
                Instant toInstant = filter.to().atTime(23, 59, 59).atOffset(ZoneOffset.UTC).toInstant();
                predicates.add(cb.lessThanOrEqualTo(root.get("validUpto"), toInstant));
            }

            if (filter.search() != null && !filter.search().isBlank()) {
                String pattern = "%" + filter.search().toLowerCase() + "%";
                Predicate titlePred = cb.like(cb.lower(root.get("title")), pattern);

                Subquery<Long> sub = query.subquery(Long.class);
                Root<DiscountEntity> subRoot = sub.from(DiscountEntity.class);
                Join<DiscountEntity, String> codeJoin = subRoot.join("itemCodes");
                sub.select(subRoot.get("id"))
                        .where(cb.and(
                                cb.equal(subRoot.get("id"), root.get("id")),
                                cb.like(cb.lower(codeJoin), pattern)
                        ));
                Predicate codePred = cb.exists(sub);
                predicates.add(cb.or(titlePred, codePred));
            }

            if (filter.status() != null) {
                Instant now = Instant.now();
                switch (filter.status()) {
                    case "ACTIVE" -> {
                        predicates.add(cb.isFalse(root.get("disabled")));
                        predicates.add(cb.lessThanOrEqualTo(root.get("validFrom"), now));
                        predicates.add(cb.greaterThanOrEqualTo(root.get("validUpto"), now));
                    }
                    case "SCHEDULED" -> {
                        predicates.add(cb.isFalse(root.get("disabled")));
                        predicates.add(cb.greaterThan(root.get("validFrom"), now));
                    }
                    case "EXPIRED" -> {
                        predicates.add(cb.isFalse(root.get("disabled")));
                        predicates.add(cb.lessThan(root.get("validUpto"), now));
                    }
                    case "DISABLED" -> predicates.add(cb.isTrue(root.get("disabled")));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
