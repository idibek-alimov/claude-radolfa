import type { User } from "@/entities/user";

export type { User };

export interface OrderItem {
    productName: string;
    quantity: number;
    price: number;
    skuId?: number | null;
    listingVariantId?: number | null;
    imageUrl?: string | null;
}

export interface Order {
    id: number;
    status: 'PENDING' | 'PAID' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';
    totalAmount: number;
    items: OrderItem[];
    createdAt: string;
}

export interface UpdateProfileRequest {
    name: string;
    email: string;
}
