import { useQuery } from "@tanstack/react-query";
import apiClient from "@/shared/api/axios";
import type { Pickpoint } from "./model/types";

export function useActivePickpoints() {
  return useQuery({
    queryKey: ["pickpoints"],
    queryFn: () =>
      apiClient.get<Pickpoint[]>("/api/v1/pickpoints").then((r) => r.data),
    staleTime: 5 * 60 * 1000,
  });
}
