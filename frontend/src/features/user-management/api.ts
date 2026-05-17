import apiClient from "@/shared/api/axios";
import type { PaginatedResponse } from "@/shared/api/types";
import type { UserDto, ToggleStatusParams, ChangeRoleParams, AssignTierParams, SetLoyaltyPermanentParams } from "./types";

export interface PickpointOption {
  id: number;
  name: string;
  address: string;
  active: boolean;
}

export interface CreateCourierBody {
  phone: string;
  name: string;
  vehicleType: string;
  maxPayloadKg: number;
  maxLengthCm?: number | null;
  maxWidthCm?: number | null;
  maxHeightCm?: number | null;
}

export interface CreatePickpointStaffBody {
  phone: string;
  name: string;
  pickpointId: number;
}

export interface UpdateCourierBody {
  vehicleType: string;
  maxPayloadKg: number;
  maxLengthCm?: number | null;
  maxWidthCm?: number | null;
  maxHeightCm?: number | null;
}

export async function fetchUsers(
  search: string,
  page: number,
  size: number = 20,
  roles?: string[]
): Promise<PaginatedResponse<UserDto>> {
  const params: Record<string, unknown> = { search, page, size };
  if (roles && roles.length > 0) {
    params.role = roles;
  }
  const { data } = await apiClient.get<PaginatedResponse<UserDto>>("/api/v1/users", {
    params,
    paramsSerializer: (p) => {
      const sp = new URLSearchParams();
      for (const [key, val] of Object.entries(p)) {
        if (Array.isArray(val)) {
          val.forEach((v) => sp.append(key, v));
        } else {
          sp.append(key, String(val));
        }
      }
      return sp.toString();
    },
  });
  return data;
}

export async function toggleUserStatus({
  userId,
  enabled,
}: ToggleStatusParams): Promise<UserDto> {
  const { data } = await apiClient.patch<UserDto>(
    `/api/v1/users/${userId}/status`,
    { enabled }
  );
  return data;
}

export async function changeUserRole({
  userId,
  role,
}: ChangeRoleParams): Promise<UserDto> {
  const { data } = await apiClient.patch<UserDto>(
    `/api/v1/users/${userId}/role`,
    { role }
  );
  return data;
}

export async function assignUserTier({
  userId,
  tierId,
}: AssignTierParams): Promise<UserDto> {
  const { data } = await apiClient.patch<UserDto>(
    `/api/v1/users/${userId}/tier`,
    { tierId }
  );
  return data;
}

export async function setLoyaltyPermanent({
  userId,
  permanent,
}: SetLoyaltyPermanentParams): Promise<UserDto> {
  const { data } = await apiClient.patch<UserDto>(
    `/api/v1/users/${userId}/loyalty-permanent`,
    null,
    { params: { permanent } }
  );
  return data;
}

export async function fetchPickpoints(): Promise<PickpointOption[]> {
  const { data } = await apiClient.get<PickpointOption[]>("/api/v1/admin/pickpoints");
  return data;
}

export async function createCourier(body: CreateCourierBody): Promise<UserDto> {
  const { data } = await apiClient.post<UserDto>("/api/v1/admin/users/couriers", body);
  return data;
}

export async function createPickpointStaff(body: CreatePickpointStaffBody): Promise<UserDto> {
  const { data } = await apiClient.post<UserDto>("/api/v1/admin/users/pickpoint-staff", body);
  return data;
}

export async function updateCourierDetails(id: number, body: UpdateCourierBody): Promise<UserDto> {
  const { data } = await apiClient.patch<UserDto>(`/api/v1/admin/users/${id}/courier-details`, body);
  return data;
}

export async function reassignPickpointStaff(id: number, pickpointId: number): Promise<UserDto> {
  const { data } = await apiClient.patch<UserDto>(`/api/v1/admin/users/${id}/pickpoint-assignment`, { pickpointId });
  return data;
}
