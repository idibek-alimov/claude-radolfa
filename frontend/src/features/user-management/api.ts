import apiClient from "@/shared/api/axios";
import type { UserDto, PageResult, ToggleStatusParams, ChangeRoleParams } from "./types";

export async function fetchUsers(
  search: string,
  page: number,
  size: number = 20
): Promise<PageResult<UserDto>> {
  const { data } = await apiClient.get<PageResult<UserDto>>("/api/v1/users", {
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
    null,
    { params: { enabled } }
  );
  return data;
}

export async function changeUserRole({
  userId,
  role,
}: ChangeRoleParams): Promise<UserDto> {
  const { data } = await apiClient.patch<UserDto>(
    `/api/v1/users/${userId}/role`,
    null,
    { params: { role } }
  );
  return data;
}
