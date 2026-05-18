import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import apiClient from "@/shared/api/axios";
import type { PaginatedResponse } from "@/shared/api/types";
import type { OrderStatus } from "@/entities/order/model/types";
import type { PickpointOrder } from "@/entities/user";
import type {
  CustomerReturn,
  CustomerReturnStatus,
  ReturnableOrder,
  CreateCustomerReturnPayload,
} from "@/entities/pickpoint";

// ── Order queries ─────────────────────────────────────────────────────────────

export function usePickpointOrders(statuses: OrderStatus[], page = 1, size = 20) {
  return useQuery({
    queryKey: ["pickpoint-orders", statuses.join(","), page, size],
    queryFn: () =>
      apiClient
        .get<PaginatedResponse<PickpointOrder>>("/api/v1/pickpoint/orders", {
          params: { statuses: statuses.join(","), page, size },
        })
        .then((r) => r.data),
    enabled: statuses.length > 0,
  });
}

export function useLookUpOrderForReturn(orderId: number | null) {
  return useQuery({
    queryKey: ["pickpoint-return-lookup", orderId],
    queryFn: () =>
      apiClient
        .get<ReturnableOrder>(`/api/v1/pickpoint/orders/${orderId}/for-return`)
        .then((r) => r.data),
    enabled: orderId != null,
    retry: false,
  });
}

// ── Customer-return queries ───────────────────────────────────────────────────

export function usePickpointCustomerReturns(
  status: CustomerReturnStatus,
  page = 1,
  size = 20,
) {
  return useQuery({
    queryKey: ["pickpoint-customer-returns", status, page, size],
    queryFn: () =>
      apiClient
        .get<PaginatedResponse<CustomerReturn>>("/api/v1/pickpoint/customer-returns", {
          params: { status, page, size },
        })
        .then((r) => r.data),
  });
}

// ── Order mutations ───────────────────────────────────────────────────────────

export function useConfirmArrival() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (orderId: number) =>
      apiClient.post(`/api/v1/pickpoint/orders/${orderId}/confirm-arrival`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["pickpoint-orders"] }),
  });
}

export function useVerifyPickup() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (code: string) =>
      apiClient.post("/api/v1/pickpoint/verify-pickup", { code }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["pickpoint-orders"] }),
  });
}

export function useInitiateReturn() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (orderId: number) =>
      apiClient.post(`/api/v1/pickpoint/orders/${orderId}/initiate-return`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["pickpoint-orders"] }),
  });
}

export function useConfirmReturnedToWarehouse() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (orderId: number) =>
      apiClient.post(`/api/v1/pickpoint/orders/${orderId}/confirm-returned`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["pickpoint-orders"] }),
  });
}

// ── Customer-return mutations ─────────────────────────────────────────────────

export function useCreateCustomerReturn() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateCustomerReturnPayload) =>
      apiClient
        .post<CustomerReturn>("/api/v1/pickpoint/customer-returns", payload)
        .then((r) => r.data),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: ["pickpoint-customer-returns"] }),
  });
}

export function useConfirmCustomerReturnSent() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (returnId: number) =>
      apiClient.post(`/api/v1/pickpoint/customer-returns/${returnId}/confirm-sent`),
    onSuccess: () =>
      qc.invalidateQueries({ queryKey: ["pickpoint-customer-returns"] }),
  });
}
