package tj.radolfa.infrastructure.persistence.mappers;

import java.time.Instant;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Product;
import tj.radolfa.infrastructure.persistence.entity.ProductEntity;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-08T18:51:30+0500",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.18 (Ubuntu)"
)
@Component
public class ProductMapperImpl implements ProductMapper {

    @Override
    public Product toProduct(ProductEntity entity) {
        if ( entity == null ) {
            return null;
        }

        boolean topSelling = false;
        List<String> images = null;
        Long id = null;
        String erpId = null;
        String name = null;
        Money price = null;
        Integer stock = null;
        String webDescription = null;
        Instant lastErpSyncAt = null;

        topSelling = entity.isTopSelling();
        images = imagesToUrls( entity.getImages() );
        id = entity.getId();
        erpId = entity.getErpId();
        name = entity.getName();
        price = bigDecimalToMoney( entity.getPrice() );
        stock = entity.getStock();
        webDescription = entity.getWebDescription();
        lastErpSyncAt = entity.getLastErpSyncAt();

        Product product = new Product( id, erpId, name, price, stock, webDescription, topSelling, images, lastErpSyncAt );

        return product;
    }

    @Override
    public ProductEntity toEntity(Product product) {
        if ( product == null ) {
            return null;
        }

        ProductEntity productEntity = new ProductEntity();

        productEntity.setId( product.getId() );
        productEntity.setErpId( product.getErpId() );
        productEntity.setName( product.getName() );
        productEntity.setPrice( moneyToBigDecimal( product.getPrice() ) );
        productEntity.setStock( product.getStock() );
        productEntity.setWebDescription( product.getWebDescription() );
        productEntity.setTopSelling( product.isTopSelling() );
        productEntity.setLastErpSyncAt( product.getLastErpSyncAt() );

        return productEntity;
    }
}
