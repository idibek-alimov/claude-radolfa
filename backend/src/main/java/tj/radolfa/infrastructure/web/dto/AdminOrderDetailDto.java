package tj.radolfa.infrastructure.web.dto;

import tj.radolfa.application.ports.in.order.GetAdminOrderDetailUseCase;
import tj.radolfa.domain.model.Order;
import tj.radolfa.domain.model.Pickpoint;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AdminOrderDetailDto(
        Long id,
        String userPhone,
        String userName,
        String status,
        BigDecimal totalAmount,
        Instant createdAt,
        int loyaltyPointsRedeemed,
        int loyaltyPointsAwarded,
        List<AdminOrderItemDto> items,
        String deliveryType,
        String deliveryAddress,
        String preferredTimeWindow,
        Long pickpointId,
        String pickpointName,
        String pickpointAddress,
        Long courierId,
        String courierName,
        String trackingNumber,
        LocalDate estimatedDeliveryDate,
        Instant shippedAt,
        Instant deliveredAt,
        Instant cancelledAt,
        Instant refundedAt) {

    public static AdminOrderDetailDto from(GetAdminOrderDetailUseCase.Result result,
                                           List<AdminOrderItemDto> enrichedItems) {
        Order order = result.order();
        Pickpoint pp = result.pickpoint().orElse(null);

        return new AdminOrderDetailDto(
                order.id(),
                result.userPhone(),
                result.userName(),
                order.status().name(),
                order.totalAmount().amount(),
                order.createdAt(),
                order.loyaltyPointsRedeemed(),
                order.loyaltyPointsAwarded(),
                enrichedItems,
                order.deliveryType() != null ? order.deliveryType().name() : null,
                order.deliveryAddress(),
                order.preferredTimeWindow(),
                order.pickpointId(),
                pp != null ? pp.name()    : null,
                pp != null ? pp.address() : null,
                order.courierId(),
                result.courierName(),
                order.trackingNumber(),
                order.estimatedDeliveryDate(),
                order.shippedAt(),
                order.deliveredAt(),
                order.cancelledAt(),
                order.refundedAt()
        );
    }
}
