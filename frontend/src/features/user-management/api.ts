import apiClient from "@/shared/api/axios";
import type { PaginatedResponse } from "@/shared/api/types";
import type { UserDto, ToggleStatusParams, ChangeRoleParams, AssignTierParams, SetLoyaltyPermanentParams } from "./types";

export async function fetchUsers(
  search: string,
  page: number,
  size: number = 20
): Promise<PaginatedResponse<UserDto>> {
  const { data } = await apiClient.get<PaginatedResponse<UserDto>>("/api/v1/users", {
    params: { search, page, size },
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
