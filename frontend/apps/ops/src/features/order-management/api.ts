import { useQuery } from "@tanstack/react-query";
import apiClient from "@radolfa/shared/api/axios";
import type { CustomerReturn } from "@/entities/pickpoint";

export function useAdminCustomerReturnsForOrder(orderId: number) {
  return useQuery({
    queryKey: ["admin-order-customer-returns", orderId],
    queryFn: () =>
      apiClient
        .get<CustomerReturn[]>(`/api/v1/admin/orders/${orderId}/customer-returns`)
        .then((r) => r.data),
    enabled: !!orderId,
  });
}
