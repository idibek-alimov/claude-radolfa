import { useQuery } from "@tanstack/react-query";
import { keepPreviousData } from "@tanstack/react-query";
import apiClient from "@/shared/api/axios";
import type { PaginatedResponse } from "@/shared/api/types";
import { Order, UpdateProfileRequest, User } from "./types";
import type { MyReturn } from "./types";

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

export async function getMyReturns(
  page: number,
  size: number
): Promise<PaginatedResponse<MyReturn>> {
  const response = await apiClient.get<PaginatedResponse<MyReturn>>(
    "/api/v1/orders/my-returns",
    { params: { page, size } }
  );
  return response.data;
}

export function useMyReturns(page: number, size: number = 10) {
  return useQuery({
    queryKey: ["my-returns", page, size],
    queryFn: () => getMyReturns(page, size),
    placeholderData: keepPreviousData,
  });
}
