package tj.radolfa.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public record Order(
        Long id,
        Long userId,
        String externalOrderId,
        OrderStatus status,
        Money totalAmount,
        List<OrderItem> items,
        Instant createdAt,
        int loyaltyPointsRedeemed,
        int loyaltyPointsAwarded,
        DeliveryType deliveryType,
        String deliveryAddress,
        String preferredTimeWindow,
        Long pickpointId,
        Long courierId,
        String trackingNumber,
        LocalDate estimatedDeliveryDate,
        Instant shippedAt,
        Instant deliveredAt,
        Instant cancelledAt,
        Instant refundedAt,
        Instant outForDeliveryAt,
        Instant deliveryAttemptedAt,
        int deliveryAttemptCount,
        DeliveryAttemptReason deliveryAttemptReason,
        String deliveryPhotoUrl) {

    public Order {
        items = items == null ? List.of() : Collections.unmodifiableList(items);
    }

    public Builder toBuilder() {
        return new Builder()
                .id(id).userId(userId).externalOrderId(externalOrderId)
                .status(status).totalAmount(totalAmount).items(items).createdAt(createdAt)
                .loyaltyPointsRedeemed(loyaltyPointsRedeemed).loyaltyPointsAwarded(loyaltyPointsAwarded)
                .deliveryType(deliveryType).deliveryAddress(deliveryAddress)
                .preferredTimeWindow(preferredTimeWindow).pickpointId(pickpointId)
                .courierId(courierId).trackingNumber(trackingNumber)
                .estimatedDeliveryDate(estimatedDeliveryDate)
                .shippedAt(shippedAt).deliveredAt(deliveredAt)
                .cancelledAt(cancelledAt).refundedAt(refundedAt)
                .outForDeliveryAt(outForDeliveryAt).deliveryAttemptedAt(deliveryAttemptedAt)
                .deliveryAttemptCount(deliveryAttemptCount).deliveryAttemptReason(deliveryAttemptReason)
                .deliveryPhotoUrl(deliveryPhotoUrl);
    }

    public static final class Builder {
        private Long id;
        private Long userId;
        private String externalOrderId;
        private OrderStatus status;
        private Money totalAmount;
        private List<OrderItem> items;
        private Instant createdAt;
        private int loyaltyPointsRedeemed;
        private int loyaltyPointsAwarded;
        private DeliveryType deliveryType;
        private String deliveryAddress;
        private String preferredTimeWindow;
        private Long pickpointId;
        private Long courierId;
        private String trackingNumber;
        private LocalDate estimatedDeliveryDate;
        private Instant shippedAt;
        private Instant deliveredAt;
        private Instant cancelledAt;
        private Instant refundedAt;
        private Instant outForDeliveryAt;
        private Instant deliveryAttemptedAt;
        private int deliveryAttemptCount;
        private DeliveryAttemptReason deliveryAttemptReason;
        private String deliveryPhotoUrl;

        public Builder id(Long v)                              { this.id = v; return this; }
        public Builder userId(Long v)                          { this.userId = v; return this; }
        public Builder externalOrderId(String v)               { this.externalOrderId = v; return this; }
        public Builder status(OrderStatus v)                   { this.status = v; return this; }
        public Builder totalAmount(Money v)                    { this.totalAmount = v; return this; }
        public Builder items(List<OrderItem> v)                { this.items = v; return this; }
        public Builder createdAt(Instant v)                    { this.createdAt = v; return this; }
        public Builder loyaltyPointsRedeemed(int v)            { this.loyaltyPointsRedeemed = v; return this; }
        public Builder loyaltyPointsAwarded(int v)             { this.loyaltyPointsAwarded = v; return this; }
        public Builder deliveryType(DeliveryType v)            { this.deliveryType = v; return this; }
        public Builder deliveryAddress(String v)               { this.deliveryAddress = v; return this; }
        public Builder preferredTimeWindow(String v)           { this.preferredTimeWindow = v; return this; }
        public Builder pickpointId(Long v)                     { this.pickpointId = v; return this; }
        public Builder courierId(Long v)                       { this.courierId = v; return this; }
        public Builder trackingNumber(String v)                { this.trackingNumber = v; return this; }
        public Builder estimatedDeliveryDate(LocalDate v)      { this.estimatedDeliveryDate = v; return this; }
        public Builder shippedAt(Instant v)                    { this.shippedAt = v; return this; }
        public Builder deliveredAt(Instant v)                  { this.deliveredAt = v; return this; }
        public Builder cancelledAt(Instant v)                  { this.cancelledAt = v; return this; }
        public Builder refundedAt(Instant v)                   { this.refundedAt = v; return this; }
        public Builder outForDeliveryAt(Instant v)             { this.outForDeliveryAt = v; return this; }
        public Builder deliveryAttemptedAt(Instant v)          { this.deliveryAttemptedAt = v; return this; }
        public Builder deliveryAttemptCount(int v)             { this.deliveryAttemptCount = v; return this; }
        public Builder deliveryAttemptReason(DeliveryAttemptReason v) { this.deliveryAttemptReason = v; return this; }
        public Builder deliveryPhotoUrl(String v)              { this.deliveryPhotoUrl = v; return this; }

        public Order build() {
            return new Order(id, userId, externalOrderId, status, totalAmount, items, createdAt,
                    loyaltyPointsRedeemed, loyaltyPointsAwarded,
                    deliveryType, deliveryAddress, preferredTimeWindow, pickpointId,
                    courierId, trackingNumber, estimatedDeliveryDate,
                    shippedAt, deliveredAt, cancelledAt, refundedAt,
                    outForDeliveryAt, deliveryAttemptedAt, deliveryAttemptCount,
                    deliveryAttemptReason, deliveryPhotoUrl);
        }
    }
}
