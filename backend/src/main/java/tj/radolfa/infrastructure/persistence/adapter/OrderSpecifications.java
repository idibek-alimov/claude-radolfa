package tj.radolfa.infrastructure.persistence.adapter;

import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import tj.radolfa.domain.model.OrderStatus;
import tj.radolfa.infrastructure.persistence.entity.OrderEntity;
import tj.radolfa.infrastructure.persistence.entity.UserEntity;

import java.util.ArrayList;
import java.util.List;

class OrderSpecifications {

    private OrderSpecifications() {}

    /**
     * Builds a Specification for the admin orders list.
     *
     * <p>For the result query it performs a fetch-join on {@code user} (safe for pagination
     * because {@code user} is {@code @ManyToOne} — no collection fetch involved).
     * For the count query it falls back to a plain join.
     *
     * @param search       matches phone (LIKE) or order id (exact). Null/blank = all.
     * @param statusFilter null = all statuses.
     */
    static Specification<OrderEntity> adminSearch(String search, OrderStatus statusFilter) {
        return (root, query, cb) -> {

            Join<OrderEntity, UserEntity> userJoin;
            if (query.getResultType() != Long.class) {
                query.distinct(true);
                @SuppressWarnings("unchecked")
                Fetch<OrderEntity, UserEntity> fetch = root.fetch("user", JoinType.LEFT);
                userJoin = (Join<OrderEntity, UserEntity>) fetch;
            } else {
                userJoin = root.join("user", JoinType.LEFT);
            }

            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                Predicate phoneLike = cb.like(cb.lower(userJoin.get("phone")), pattern);
                try {
                    Long id = Long.parseLong(search.trim());
                    predicates.add(cb.or(phoneLike, cb.equal(root.get("id"), id)));
                } catch (NumberFormatException ignored) {
                    predicates.add(phoneLike);
                }
            }

            if (statusFilter != null) {
                predicates.add(cb.equal(root.get("status"), statusFilter));
            }

            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
