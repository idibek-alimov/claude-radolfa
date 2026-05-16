import type { LoyaltyProfile } from "@/entities/loyalty/model/types";
import type { OrderStatus } from "@/entities/order/model/types";

/**
 * Authorised roles.
 * Source: tj.radolfa.domain.model.UserRole
 */
export enum UserRole {
  USER = "USER",
  MANAGER = "MANAGER",
  ADMIN = "ADMIN",
  COURIER = "COURIER",
  PICKPOINT_STAFF = "PICKPOINT_STAFF",
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
  loyalty: LoyaltyProfile | null;
  enabled: boolean;
  pickpointId?: number | null;
  vehicleType?: "BICYCLE" | "MOTORCYCLE" | "CAR" | "VAN" | null;
  maxPayloadKg?: number | null;
}

export interface CourierSummary {
  id: number;
  name: string;
  phone: string;
  vehicleType: string;
  maxPayloadKg: number | null;
}

export interface CourierOrder {
  orderId: number;
  customerFirstName: string;
  customerPhone: string;
  deliveryAddress: string;
  preferredTimeWindow: string | null;
  status: OrderStatus;
  deliveryAttemptCount: number;
  totalItemCount: number;
  totalWeightKg: number | null;
  shippedAt: string | null;
  outForDeliveryAt: string | null;
}

export interface PickpointOrder {
  orderId: number;
  customerFirstName: string;
  status: OrderStatus;
  readyAt: string;
  expiresAt: string;
  daysUntilExpiry: number;
}

export interface CourierFleetEntry {
  courierId: number;
  name: string;
  vehicleType: string | null;
  maxPayloadKg: number | null;
  deliveredToday: number;
  inTransit: number;
  attempted: number;
}
