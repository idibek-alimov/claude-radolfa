/** Auth feature types */

export interface LoginRequest {
  phone: string;
}

export interface VerifyOtpRequest {
  phone: string;
  otp: string;
}

export interface User {
  id: number;
  phone: string;
  role: "USER" | "MANAGER" | "SYSTEM";
  name?: string;
  email?: string;
  loyaltyPoints: number;
  enabled: boolean;
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
