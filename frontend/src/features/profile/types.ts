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

export interface User {
    id: number;
    phone: string;
    role: "USER" | "MANAGER" | "SYSTEM";
    name?: string;
    email?: string;
    loyaltyPoints: number;
    enabled: boolean;
}
