import type { User } from "@/entities/user";
import type { OrderStatus } from "@/entities/order/model/types";

export type { User, OrderStatus };

export interface OrderItem {
    productName: string;
    quantity: number;
    price: number;
    skuId?: number | null;
    listingVariantId?: number | null;
    imageUrl?: string | null;
    skuCode?: string | null;
    sizeLabel?: string | null;
    slug?: string | null;
    hasReviewed: boolean;
}

export interface Order {
    id: number;
    status: OrderStatus;
    totalAmount: number;
    items: OrderItem[];
    createdAt: string;
    loyaltyPointsRedeemed: number;
    loyaltyPointsAwarded: number;
    deliveryType: 'HOME' | 'PICKPOINT' | null;
    courierName: string | null;
    trackingNumber: string | null;
    estimatedDeliveryDate: string | null;
    pickpointName: string | null;
    pickpointAddress: string | null;
    deliveryAddress: string | null;
}

export interface UpdateProfileRequest {
    name: string;
    email: string;
}
