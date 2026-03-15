package tj.radolfa.infrastructure.persistence.adapter;

import org.springframework.stereotype.Component;
import tj.radolfa.application.ports.out.DeleteDiscountPort;
import tj.radolfa.application.ports.out.LoadDiscountPort;
import tj.radolfa.application.ports.out.SaveDiscountPort;
import tj.radolfa.domain.model.Discount;
import tj.radolfa.infrastructure.persistence.entity.DiscountEntity;
import tj.radolfa.infrastructure.persistence.mappers.DiscountMapper;
import tj.radolfa.infrastructure.persistence.repository.DiscountRepository;

import java.util.List;
import java.util.Optional;

@Component
public class DiscountAdapter implements LoadDiscountPort, SaveDiscountPort, DeleteDiscountPort {

    private final DiscountRepository repository;
    private final DiscountMapper mapper;

    public DiscountAdapter(DiscountRepository repository, DiscountMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    // ---- LoadDiscountPort ----

    @Override
    public Optional<Discount> findByErpPricingRuleId(String erpPricingRuleId) {
        return repository.findByErpPricingRuleId(erpPricingRuleId)
                .map(mapper::toDomain);
    }

    @Override
    public List<Discount> findActiveByItemCode(String itemCode) {
        return repository.findActiveByItemCode(itemCode).stream()
                .map(mapper::toDomain)
                .toList();
    }

    // ---- SaveDiscountPort ----

    @Override
    public Discount save(Discount discount) {
        DiscountEntity entity;

        if (discount.id() != null) {
            entity = repository.findById(discount.id())
                    .orElseThrow(() -> new IllegalStateException(
                            "Discount not found: " + discount.id()));
            entity.setErpPricingRuleId(discount.erpPricingRuleId());
            entity.setItemCode(discount.itemCode());
            entity.setDiscountValue(discount.discountValue());
            entity.setValidFrom(discount.validFrom());
            entity.setValidUpto(discount.validUpto());
            entity.setDisabled(discount.disabled());
        } else {
            entity = mapper.toEntity(discount);
        }

        return mapper.toDomain(repository.save(entity));
    }

    // ---- DeleteDiscountPort ----

    @Override
    public void deleteByErpPricingRuleId(String erpPricingRuleId) {
        repository.deleteByErpPricingRuleId(erpPricingRuleId);
    }
}
