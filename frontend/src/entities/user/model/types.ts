import type { LoyaltyProfile } from "@/entities/loyalty/model/types";

/**
 * Authorised roles.
 * Source: tj.radolfa.domain.model.UserRole
 */
export enum UserRole {
  USER = "USER",
  MANAGER = "MANAGER",
  ADMIN = "ADMIN",
}

/**
 * Immutable user shape as returned by the backend.
 * Source: tj.radolfa.domain.model.User
 */
export interface User {
  id: number | null;
  phone: string;
  role: UserRole;
  name?: string;
  email?: string;
  loyalty: LoyaltyProfile;
  enabled: boolean;
}
