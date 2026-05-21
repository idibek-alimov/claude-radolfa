import { useQuery } from "@tanstack/react-query";
import apiClient from "@/shared/api/axios";
import type { MyOrderDetail } from "./types";

async function getMyOrderDetail(id: string): Promise<MyOrderDetail> {
  const response = await apiClient.get<MyOrderDetail>(`/api/v1/orders/${id}`);
  return response.data;
}

export function useMyOrderDetail(id: string) {
  return useQuery({
    queryKey: ["my-order-detail", id],
    queryFn: () => getMyOrderDetail(id),
    enabled: !!id,
  });
}
