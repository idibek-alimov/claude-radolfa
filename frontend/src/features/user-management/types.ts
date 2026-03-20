import type { LoyaltyProfile } from "@/entities/loyalty";
import type { PaginatedResponse } from "@/shared/api/types";

export type { PaginatedResponse };

export interface UserDto {
  id: number;
  phone: string;
  role: "USER" | "MANAGER" | "ADMIN";
  name?: string;
  email?: string;
  loyalty: LoyaltyProfile;
  enabled: boolean;
}

export interface ToggleStatusParams {
  userId: number;
  enabled: boolean;
}

export interface ChangeRoleParams {
  userId: number;
  role: "USER" | "MANAGER";
}
