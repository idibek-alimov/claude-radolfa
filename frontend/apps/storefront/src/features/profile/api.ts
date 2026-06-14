import { useQuery, useInfiniteQuery } from "@tanstack/react-query";
import { keepPreviousData } from "@tanstack/react-query";
import apiClient from "@radolfa/shared/api/axios";
import type { PaginatedResponse } from "@radolfa/shared/api/types";
import { Order, UpdateProfileRequest, User } from "./types";
import type { MyReturn } from "./types";
import type { MyOrdersSummary } from "@/entities/order";

/** Server-side order-list filter — matches the `/orders/my-orders` whitelist (file 01 Phase 4). */
export type OrderFilter = "all" | "progress" | "delivered" | "returns";

export async function getMyOrders(
  page: number,
  size: number = 10,
  filter: OrderFilter = "all"
): Promise<PaginatedResponse<Order>> {
  const response = await apiClient.get<PaginatedResponse<Order>>(
    "/api/v1/orders/my-orders",
    { params: { page, size, filter } }
  );
  return response.data;
}

export function useMyOrders(page: number, size: number = 10, filter: OrderFilter = "all") {
  return useQuery({
    queryKey: ["my-orders", page, size, filter],
    queryFn: () => getMyOrders(page, size, filter),
    placeholderData: keepPreviousData,
  });
}

/** Infinite "Show more" list for the Orders route — server-side filtered, 1-based pages. */
export function useMyOrdersInfinite(filter: OrderFilter, size: number = 10) {
  return useInfiniteQuery({
    queryKey: ["my-orders", "infinite", filter, size],
    queryFn: ({ pageParam }) => getMyOrders(pageParam, size, filter),
    initialPageParam: 1,
    getNextPageParam: (lastPage) => (lastPage.last ? undefined : lastPage.number + 2),
    placeholderData: keepPreviousData,
  });
}

export async function getMyOrdersSummary(): Promise<MyOrdersSummary> {
  const response = await apiClient.get<MyOrdersSummary>(
    "/api/v1/orders/my-orders/summary"
  );
  return response.data;
}

export function useOrderSummary() {
  return useQuery({
    queryKey: ["my-orders-summary"],
    queryFn: getMyOrdersSummary,
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
