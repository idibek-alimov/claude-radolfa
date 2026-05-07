package tj.radolfa.infrastructure.persistence.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import tj.radolfa.domain.model.Money;
import tj.radolfa.domain.model.Payment;
import tj.radolfa.infrastructure.persistence.entity.PaymentEntity;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    @Mapping(source = "order.id", target = "orderId")
    Payment toPayment(PaymentEntity entity);

    @Mapping(target = "order", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PaymentEntity toEntity(Payment payment);

    /** Updates an existing managed entity in-place, preserving version/audit fields. */
    @Mapping(target = "order", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(Payment payment, @MappingTarget PaymentEntity entity);

    // ---- Money <-> BigDecimal bridge --------------------------------

    default Money bigDecimalToMoney(BigDecimal value) {
        return Money.of(value);
    }

    default BigDecimal moneyToBigDecimal(Money money) {
        return money != null ? money.amount() : null;
    }
}
