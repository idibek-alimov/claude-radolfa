import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import apiClient from "@radolfa/shared/api/axios";
import type { Address, AddressRequest } from "../model/types";

/** GET /addresses returns the full address book (owner-scoped, no pagination —
 *  by design every customer has a handful of addresses). */
export const getMyAddresses = (): Promise<Address[]> =>
  apiClient.get<Address[]>("/api/v1/addresses").then((r) => r.data);

export function useMyAddresses() {
  return useQuery({
    queryKey: ["addresses"],
    queryFn: getMyAddresses,
  });
}

export const createAddress = (payload: AddressRequest): Promise<Address> =>
  apiClient.post<Address>("/api/v1/addresses", payload).then((r) => r.data);

export const updateAddress = (id: number, payload: AddressRequest): Promise<Address> =>
  apiClient.put<Address>(`/api/v1/addresses/${id}`, payload).then((r) => r.data);

export const deleteAddress = (id: number): Promise<void> =>
  apiClient.delete(`/api/v1/addresses/${id}`).then(() => undefined);

export const setDefaultAddress = (id: number): Promise<void> =>
  apiClient.patch(`/api/v1/addresses/${id}/default`).then(() => undefined);

export function useCreateAddress() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: createAddress,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["addresses"] }),
  });
}

export function useUpdateAddress() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: AddressRequest }) =>
      updateAddress(id, payload),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["addresses"] }),
  });
}

export function useDeleteAddress() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: deleteAddress,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["addresses"] }),
  });
}

export function useSetDefaultAddress() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: setDefaultAddress,
    onSuccess: () => qc.invalidateQueries({ queryKey: ["addresses"] }),
  });
}
