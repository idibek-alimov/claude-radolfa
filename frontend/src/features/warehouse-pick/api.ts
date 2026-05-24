import { keepPreviousData, useQuery } from "@tanstack/react-query";
import apiClient from "@/shared/api/axios";
import type { PaginatedResponse } from "@/shared/api/types";
import type { PickQueueItem } from "./types";

export function usePickQueue(page: number, search: string) {
  return useQuery({
    queryKey: ["warehouse-pick-queue", page, search],
    queryFn: () =>
      apiClient
        .get<PaginatedResponse<PickQueueItem>>("/api/v1/admin/warehouse/pick-queue", {
          params: { page, size: 20, search },
        })
        .then((r) => r.data),
    placeholderData: keepPreviousData,
  });
}
