import { useQuery, keepPreviousData } from "@tanstack/react-query";
import apiClient from "@radolfa/shared/api/axios";
import type { PaginatedResponse } from "@radolfa/shared/api/types";
import type { LoyaltyTier, LoyaltyLedgerEntry } from "./model/types";

export function fetchReviewReward(): Promise<{ points: number }> {
  return apiClient
    .get<{ points: number }>("/api/v1/loyalty-tiers/review-reward")
    .then((r) => r.data);
}

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

/**
 * Paginated loyalty ledger for a single user.
 * Endpoint is 0-based (Spring @PageableDefault), so we send page - 1.
 * Gated: MANAGER + ADMIN.
 */
export function useUserLoyaltyLedger(userId: number | null, page: number) {
  return useQuery({
    queryKey: ["user-loyalty-ledger", userId, page],
    queryFn: () =>
      apiClient
        .get<PaginatedResponse<LoyaltyLedgerEntry>>(
          `/api/v1/users/${userId}/loyalty-ledger`,
          { params: { page: page - 1, size: 20 } },
        )
        .then((r) => r.data),
    placeholderData: keepPreviousData,
    enabled: !!userId,
  });
}

/**
 * ADMIN-only manual credit / debit.
 * delta > 0 = credit, delta < 0 = debit. Non-zero + non-blank reason required.
 * Returns { balance: number } — the user's new total after the adjustment.
 */
export function adjustUserPoints(params: {
  userId: number;
  delta: number;
  reason: string;
}): Promise<{ balance: number }> {
  return apiClient
    .post<{ balance: number }>(
      `/api/v1/users/${params.userId}/loyalty-adjustment`,
      { delta: params.delta, reason: params.reason },
    )
    .then((r) => r.data);
}
