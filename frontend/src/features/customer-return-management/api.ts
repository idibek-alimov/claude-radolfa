import { keepPreviousData, useQuery } from "@tanstack/react-query";
import apiClient from "@/shared/api/axios";
import type { CustomerReturn } from "@/entities/pickpoint";
import type { PaginatedResponse } from "@/shared/api/types";

export function useAdminCustomerReturns(page = 1, size = 20, search = "") {
  return useQuery({
    queryKey: ["admin-customer-returns", page, size, search],
    queryFn: () =>
      apiClient
        .get<PaginatedResponse<CustomerReturn>>("/api/v1/admin/orders/customer-returns", {
          params: { page, size, search: search || undefined },
        })
        .then((r) => r.data),
    placeholderData: keepPreviousData,
  });
}
