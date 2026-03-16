package tj.radolfa.infrastructure.persistence.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.ProductTemplate;
import tj.radolfa.domain.model.ProductVariant;
import tj.radolfa.infrastructure.persistence.entity.ProductTemplateEntity;
import tj.radolfa.infrastructure.persistence.entity.ProductVariantEntity;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    // ---- ProductTemplate ----

    @Mapping(target = "variants", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "categoryName", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ProductTemplateEntity toTemplateEntity(ProductTemplate domain);

    default ProductTemplate toProductTemplate(ProductTemplateEntity entity) {
        if (entity == null) return null;
        return new ProductTemplate(
                entity.getId(),
                entity.getErpTemplateCode(),
                entity.getName(),
                entity.getCategoryName(),
                entity.getDescription(),
                entity.getAttributesDefinition(),
                entity.isActive(),
                entity.isTopSelling(),
                entity.isFeatured()
        );
    }

    // ---- ProductVariant ----

    @Mapping(target = "template", ignore = true)
    @Mapping(target = "price", source = "price", qualifiedByName = "moneyToBigDecimal")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ProductVariantEntity toVariantEntity(ProductVariant domain);

    default ProductVariant toProductVariant(ProductVariantEntity entity) {
        if (entity == null) return null;
        return new ProductVariant(
                entity.getId(),
                entity.getTemplate() != null ? entity.getTemplate().getId() : null,
                entity.getErpVariantCode(),
                entity.getAttributes(),
                Money.of(entity.getPrice()),
                entity.getStockQty(),
                entity.isActive(),
                entity.getSeoSlug(),
                entity.getLastSyncAt()
        );
    }

    // ---- Helpers ----

    @Named("moneyToBigDecimal")
    default BigDecimal moneyToBigDecimal(Money money) {
        return money != null ? money.amount() : null;
    }
}
