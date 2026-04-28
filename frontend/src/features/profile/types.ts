import type { User } from "@/entities/user";

export type { User };

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
    status: 'PENDING' | 'PAID' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';
    totalAmount: number;
    items: OrderItem[];
    createdAt: string;
    loyaltyPointsRedeemed: number;
    loyaltyPointsAwarded: number;
}

export interface UpdateProfileRequest {
    name: string;
    email: string;
}
