import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import apiClient from "@/shared/api/axios";
import type { Pickpoint, CreatePickpointPayload, UpdatePickpointPayload } from "./model/types";

export function useActivePickpoints() {
  return useQuery({
    queryKey: ["pickpoints"],
    queryFn: () =>
      apiClient.get<Pickpoint[]>("/api/v1/pickpoints").then((r) => r.data),
    staleTime: 5 * 60 * 1000,
  });
}

// ── Admin ──────────────────────────────────────────────────────────────────

export const fetchAdminPickpoints = (search?: string): Promise<Pickpoint[]> =>
  apiClient
    .get<Pickpoint[]>("/api/v1/admin/pickpoints", {
      params: search?.trim() ? { search: search.trim() } : undefined,
    })
    .then((r) => r.data);

export const createPickpoint = (payload: CreatePickpointPayload): Promise<Pickpoint> =>
  apiClient.post<Pickpoint>("/api/v1/admin/pickpoints", payload).then((r) => r.data);

export const updatePickpoint = (id: number, payload: UpdatePickpointPayload): Promise<Pickpoint> =>
  apiClient.patch<Pickpoint>(`/api/v1/admin/pickpoints/${id}`, payload).then((r) => r.data);

export function useAdminPickpoints(search?: string) {
  return useQuery({
    queryKey: ["admin-pickpoints", search ?? ""],
    queryFn: () => fetchAdminPickpoints(search),
  });
}

export function useCreatePickpoint() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: createPickpoint,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["admin-pickpoints"] });
      qc.invalidateQueries({ queryKey: ["pickpoints"] });
    },
  });
}

export function useUpdatePickpoint() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: UpdatePickpointPayload }) =>
      updatePickpoint(id, payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["admin-pickpoints"] });
      qc.invalidateQueries({ queryKey: ["pickpoints"] });
    },
  });
}
