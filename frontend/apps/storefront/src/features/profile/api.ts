import { useQuery } from "@tanstack/react-query";
import { keepPreviousData } from "@tanstack/react-query";
import apiClient from "@/shared/api/axios";
import type { PaginatedResponse } from "@/shared/api/types";
import { Order, UpdateProfileRequest, User } from "./types";
import type { MyReturn } from "./types";

export async function getMyOrders(
  page: number,
  size: number = 10
): Promise<PaginatedResponse<Order>> {
  const response = await apiClient.get<PaginatedResponse<Order>>(
    "/api/v1/orders/my-orders",
    { params: { page, size } }
  );
  return response.data;
}

export function useMyOrders(page: number, size: number = 10) {
  return useQuery({
    queryKey: ["my-orders", page, size],
    queryFn: () => getMyOrders(page, size),
    placeholderData: keepPreviousData,
  });
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

export interface ReviewProgress {
  totalOrders: number;
  reviewedOrders: number;
}

async function getMyReviewProgress(): Promise<ReviewProgress> {
  const response = await apiClient.get<ReviewProgress>(
    "/api/v1/orders/my-review-progress"
  );
  return response.data;
}

export function useMyReviewProgress() {
  return useQuery({
    queryKey: ["my-review-progress"],
    queryFn: getMyReviewProgress,
  });
}
