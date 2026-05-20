package tj.radolfa.infrastructure.persistence.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tj.radolfa.domain.model.StockReceipt;
import tj.radolfa.domain.model.StockReceiptItem;
import tj.radolfa.infrastructure.persistence.entity.StockReceiptEntity;
import tj.radolfa.infrastructure.persistence.entity.StockReceiptItemEntity;

@Mapper(componentModel = "spring")
public interface StockReceiptMapper {

    StockReceipt toDomain(StockReceiptEntity entity);

    @Mapping(target = "version",   ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    StockReceiptEntity toEntity(StockReceipt domain);

    @Mapping(source = "receipt.id", target = "receiptId")
    StockReceiptItem toDomain(StockReceiptItemEntity entity);

    @Mapping(target = "receipt",   ignore = true)
    @Mapping(target = "version",   ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    StockReceiptItemEntity toEntity(StockReceiptItem domain);
}
