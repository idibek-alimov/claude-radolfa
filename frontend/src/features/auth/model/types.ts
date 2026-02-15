/** Auth feature types */

import type { User } from "@/entities/user";

export type { User };

export interface LoginRequest {
  phone: string;
}

export interface VerifyOtpRequest {
  phone: string;
  otp: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  user: User;
}

export interface MessageResponse {
  message: string;
  success: boolean;
}
