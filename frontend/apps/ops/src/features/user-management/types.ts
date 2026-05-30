import type { LoyaltyProfile } from "@/entities/loyalty";
import type { PaginatedResponse } from "@radolfa/shared/api/types";
import { UserRole } from "@radolfa/shared/user";

export type { PaginatedResponse };

export interface UserDto {
  id: number;
  phone: string;
  role: UserRole;
  name?: string;
  email?: string;
  loyalty: LoyaltyProfile;
  enabled: boolean;
  // Courier-specific fields
  vehicleType?: string | null;
  maxPayloadKg?: number | null;
  maxLengthCm?: number | null;
  maxWidthCm?: number | null;
  maxHeightCm?: number | null;
  // Pickpoint staff field
  pickpointId?: number | null;
  pickpointName?: string | null;
}

export interface ToggleStatusParams {
  userId: number;
  enabled: boolean;
}

export interface ChangeRoleParams {
  userId: number;
  role: UserRole;
}

export interface AssignTierParams {
  userId: number;
  tierId: number;
}

export interface SetLoyaltyPermanentParams {
  userId: number;
  permanent: boolean;
}
