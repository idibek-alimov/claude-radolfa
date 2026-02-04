package tj.radolfa.infrastructure.persistence.mappers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import tj.radolfa.domain.model.Product;
import tj.radolfa.infrastructure.persistence.entity.ProductEntity;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-04T23:56:23+0500",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.17 (Ubuntu)"
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
        BigDecimal price = null;
        Integer stock = null;
        String webDescription = null;

        topSelling = entity.isTopSelling();
        List<String> list = entity.getImages();
        if ( list != null ) {
            images = new ArrayList<String>( list );
        }
        id = entity.getId();
        erpId = entity.getErpId();
        name = entity.getName();
        price = entity.getPrice();
        stock = entity.getStock();
        webDescription = entity.getWebDescription();

        Product product = new Product( id, erpId, name, price, stock, webDescription, topSelling, images );

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
        productEntity.setPrice( product.getPrice() );
        productEntity.setStock( product.getStock() );
        productEntity.setWebDescription( product.getWebDescription() );
        productEntity.setTopSelling( product.isTopSelling() );
        List<String> list = product.getImages();
        if ( list != null ) {
            productEntity.setImages( new ArrayList<String>( list ) );
        }

        return productEntity;
    }
}
