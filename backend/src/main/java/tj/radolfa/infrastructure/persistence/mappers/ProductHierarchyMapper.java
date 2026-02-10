package tj.radolfa.infrastructure.persistence.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.Sku;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantEntity;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantImageEntity;
import tj.radolfa.infrastructure.persistence.entity.ProductBaseEntity;
import tj.radolfa.infrastructure.persistence.entity.SkuEntity;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductHierarchyMapper {

    // ---- ProductBase ----

    @Mapping(target = "variants", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ProductBaseEntity toBaseEntity(ProductBase domain);

    @Mapping(target = ".", source = "entity")
    default ProductBase toProductBase(ProductBaseEntity entity) {
        if (entity == null) return null;
        return new ProductBase(
                entity.getId(),
                entity.getErpTemplateCode(),
                entity.getName()
        );
    }

    // ---- ListingVariant ----

    @Mapping(target = "productBase", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "skus", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ListingVariantEntity toVariantEntity(ListingVariant domain);

    default ListingVariant toListingVariant(ListingVariantEntity entity) {
        if (entity == null) return null;
        List<String> imageUrls = entity.getImages() != null
                ? entity.getImages().stream()
                    .map(ListingVariantImageEntity::getImageUrl)
                    .toList()
                : Collections.emptyList();

        return new ListingVariant(
                entity.getId(),
                entity.getProductBase() != null ? entity.getProductBase().getId() : null,
                entity.getColorKey(),
                entity.getSlug(),
                entity.getWebDescription(),
                imageUrls,
                entity.getLastSyncAt()
        );
    }

    // ---- Sku ----

    @Mapping(target = "listingVariant", ignore = true)
    @Mapping(target = "price", source = "price", qualifiedByName = "moneyToBigDecimal")
    @Mapping(target = "salePrice", source = "salePrice", qualifiedByName = "moneyToBigDecimal")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SkuEntity toSkuEntity(Sku domain);

    default Sku toSku(SkuEntity entity) {
        if (entity == null) return null;
        return new Sku(
                entity.getId(),
                entity.getListingVariant() != null ? entity.getListingVariant().getId() : null,
                entity.getErpItemCode(),
                entity.getSizeLabel(),
                entity.getStockQuantity(),
                Money.of(entity.getPrice()),
                Money.of(entity.getSalePrice()),
                entity.getSaleEndsAt()
        );
    }

    // ---- Helpers ----

    @Named("moneyToBigDecimal")
    default BigDecimal moneyToBigDecimal(Money money) {
        return money != null ? money.amount() : null;
    }
}
