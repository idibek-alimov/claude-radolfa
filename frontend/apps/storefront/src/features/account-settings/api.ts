import { useMutation } from "@tanstack/react-query";
import apiClient from "@radolfa/shared/api/axios";
import type { User } from "@radolfa/shared/user";

export interface UpdateProfileRequest {
  name: string;
  email: string;
}

/** PUT /api/v1/users/profile — name + email only (phone has its own OTP flow). */
export async function updateProfile(data: UpdateProfileRequest): Promise<User> {
  const response = await apiClient.put<User>("/api/v1/users/profile", data);
  return response.data;
}

export function useUpdateProfile() {
  return useMutation({ mutationFn: updateProfile });
}
