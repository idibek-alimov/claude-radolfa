package tj.radolfa.infrastructure.persistence.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import tj.radolfa.domain.model.ListingVariant;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.ProductAttribute;
import tj.radolfa.domain.model.ProductBase;
import tj.radolfa.domain.model.Sku;
import tj.radolfa.infrastructure.persistence.entity.ListingVariantAttributeEntity;
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
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "categoryName", ignore = true)
    @Mapping(target = "brand", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ProductBaseEntity toBaseEntity(ProductBase domain);

    default ProductBase toProductBase(ProductBaseEntity entity) {
        if (entity == null) return null;
        return new ProductBase(
                entity.getId(),
                entity.getExternalRef(),
                entity.getName(),
                entity.getCategoryName(),
                entity.getBrand() != null ? entity.getBrand().getId() : null
        );
    }

    // ---- ListingVariant ----

    @Mapping(target = "productBase", ignore = true)
    @Mapping(target = "color", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "attributes", ignore = true)
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

        List<ProductAttribute> attributes = entity.getAttributes() != null
                ? entity.getAttributes().stream()
                    .map(a -> new ProductAttribute(a.getAttrKey(), a.getAttrValue(), a.getSortOrder()))
                    .toList()
                : Collections.emptyList();

        return new ListingVariant(
                entity.getId(),
                entity.getProductBase() != null ? entity.getProductBase().getId() : null,
                entity.getColor() != null ? entity.getColor().getColorKey() : null,
                entity.getSlug(),
                entity.getWebDescription(),
                imageUrls,
                attributes,
                entity.isTopSelling(),
                entity.isFeatured(),
                entity.getLastSyncAt(),
                entity.getProductCode()
        );
    }

    // ---- Sku ----

    @Mapping(target = "listingVariant", ignore = true)
    @Mapping(target = "originalPrice", source = "price", qualifiedByName = "moneyToBigDecimal")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SkuEntity toSkuEntity(Sku domain);

    default Sku toSku(SkuEntity entity) {
        if (entity == null) return null;
        return new Sku(
                entity.getId(),
                entity.getListingVariant() != null ? entity.getListingVariant().getId() : null,
                entity.getSkuCode(),
                entity.getSizeLabel(),
                entity.getStockQuantity(),
                Money.of(entity.getOriginalPrice()),
                entity.getBarcode(),
                entity.getWeightKg(),
                entity.getWidthCm(),
                entity.getHeightCm(),
                entity.getDepthCm()
        );
    }

    // ---- Helpers ----

    @Named("moneyToBigDecimal")
    default BigDecimal moneyToBigDecimal(Money money) {
        return money != null ? money.amount() : null;
    }
}
