import type { User } from "@/entities/user";

export type { User };

export interface OrderItem {
    productName: string;
    quantity: number;
    price: number;
}

export interface Order {
    id: number;
    status: string;
    totalAmount: number;
    items: OrderItem[];
    createdAt: string;
}

export interface UpdateProfileRequest {
    name: string;
    email: string;
}
