import apiClient from "@/shared/api/axios";
import { Order, UpdateProfileRequest, User } from "./types";

export async function getMyOrders(): Promise<Order[]> {
    const response = await apiClient.get<Order[]>("/api/v1/orders/my-orders");
    return response.data;
}

export async function updateProfile(data: UpdateProfileRequest): Promise<User> {
    const response = await apiClient.put<User>("/api/v1/users/profile", data);
    return response.data;
}

export async function cancelOrder(orderId: number): Promise<Order> {
    const response = await apiClient.patch<Order>(`/api/v1/orders/${orderId}/cancel`);
    return response.data;
}
