import { apiClient } from "@radolfa/shared/api";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { getErrorMessage } from "@radolfa/shared/lib";
import type { PaginatedResponse } from "@radolfa/shared/api/types";
import type { Seller } from "@/entities/seller/model/types";

export interface SellersParams {
  search?: string;
  page: number;
  size: number;
}

export interface CreateSellerPayload {
  phone: string;
  shopName: string;
  logoUrl?: string;
  bio?: string;
}

export async function fetchSellers(
  params: SellersParams
): Promise<PaginatedResponse<Seller>> {
  const { data } = await apiClient.get<PaginatedResponse<Seller>>(
    "/api/v1/admin/sellers",
    { params: { search: params.search, page: params.page, size: params.size } }
  );
  return data;
}

export async function createSeller(payload: CreateSellerPayload): Promise<Seller> {
  const { data } = await apiClient.post<Seller>("/api/v1/admin/sellers", payload);
  return data;
}

export function useSellers(params: SellersParams) {
  return useQuery({
    queryKey: ["admin-sellers", params],
    queryFn: () => fetchSellers(params),
  });
}

export function useCreateSeller() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: createSeller,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["admin-sellers"] });
      toast.success("Seller created");
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });
}
