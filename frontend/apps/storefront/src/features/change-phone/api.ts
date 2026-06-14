import { useMutation } from "@tanstack/react-query";
import apiClient from "@radolfa/shared/api/axios";
import type { User } from "@radolfa/shared/user";
import type { MessageResponse } from "@radolfa/shared/auth";
import type { PhoneChangeRequest, PhoneChangeVerify } from "./model/types";

/** POST /api/v1/users/phone/change-request — sends an OTP to the new phone number. */
export async function requestPhoneChange(data: PhoneChangeRequest): Promise<MessageResponse> {
  const response = await apiClient.post<MessageResponse>(
    "/api/v1/users/phone/change-request",
    data
  );
  return response.data;
}

/** POST /api/v1/users/phone/change-verify — confirms the OTP and returns the updated user. */
export async function verifyPhoneChange(data: PhoneChangeVerify): Promise<User> {
  const response = await apiClient.post<User>("/api/v1/users/phone/change-verify", data);
  return response.data;
}

export function useRequestPhoneChange() {
  return useMutation({ mutationFn: requestPhoneChange });
}

export function useVerifyPhoneChange() {
  return useMutation({ mutationFn: verifyPhoneChange });
}
