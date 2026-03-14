import { useQuery } from "@tanstack/react-query";
import apiClient from "@/shared/api/axios";
import type { LoyaltyTier } from "./model/types";

export function useLoyaltyTiers() {
  return useQuery({
    queryKey: ["loyalty-tiers"],
    queryFn: () =>
      apiClient.get<LoyaltyTier[]>("/api/v1/loyalty-tiers").then((r) => r.data),
    staleTime: 5 * 60 * 1000,
  });
}

export function updateTierColor(id: number, color: string): Promise<void> {
  return apiClient
    .patch(`/api/v1/loyalty-tiers/${id}/color`, { color })
    .then(() => undefined);
}
