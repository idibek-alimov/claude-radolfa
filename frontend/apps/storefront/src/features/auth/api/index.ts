import apiClient from "@/shared/api/axios";
import type {
  LoginRequest,
  VerifyOtpRequest,
  AuthResponse,
  MessageResponse,
} from "../model/types";

/** Send OTP to phone number */
export async function sendOtp(data: LoginRequest): Promise<MessageResponse> {
  const response = await apiClient.post<MessageResponse>(
    "/api/v1/auth/login",
    data
  );
  return response.data;
}

/** Verify OTP and get JWT token */
export async function verifyOtp(data: VerifyOtpRequest): Promise<AuthResponse> {
  const response = await apiClient.post<AuthResponse>(
    "/api/v1/auth/verify",
    data
  );
  return response.data;
}
