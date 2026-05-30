// ── Inlined from entities/order/model/types.ts ───────────────────────────────
// Copied here to make @radolfa/shared self-contained (no app-slice imports).
// Each app still owns its own copy in entities/order; structural typing keeps
// them interchangeable. Regenerated from backend DTOs via the `bridge` skill.
export type OrderStatus =
  | "PENDING"
  | "PAID"
  | "PICKED"
  | "CLAIMED"
  | "SHIPPED"
  | "OUT_FOR_DELIVERY"
  | "DELIVERY_ATTEMPTED"
  | "READY_FOR_PICKUP"
  | "RETURN_INITIATED"
  | "RETURNED_TO_WAREHOUSE"
  | "DELIVERED"
  | "CANCELLED"
  | "REFUNDED"
  | "RECALL_REQUESTED";

// ── Inlined from entities/loyalty/model/types.ts ─────────────────────────────
export interface LoyaltyTier {
  id: number;
  name: string;
  discountPercentage: number;
  cashbackPercentage: number;
  minSpendRequirement: number;
  /** Lower = higher tier (1 is best). */
  displayOrder: number;
  /** Hex color for the tier, e.g. "#F59E0B". */
  color: string;
}

export interface LoyaltyEarning {
  orderId: number;
  pointsEarned: number;
  orderAmount: number;
  orderedAt: string; // ISO-8601
}

/**
 * User's loyalty profile, nested inside the User object.
 * Returned as part of GET /api/v1/users/me.
 */
export interface LoyaltyProfile {
  points: number;
  tier: LoyaltyTier | null;
  spendToNextTier: number | null;
  spendToMaintainTier: number | null;
  currentMonthSpending: number | null;
  recentEarnings: LoyaltyEarning[];
  permanent: boolean;
  floorTierName: string | null;
}

// ── User entity ───────────────────────────────────────────────────────────────

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
  WAREHOUSE_MANAGER = "WAREHOUSE_MANAGER",
  /** Frontend-only placeholder. Backend addition (seller_id columns + /api/v1/seller/* endpoints) is a separate marketplace plan. */
  SELLER = "SELLER",
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
  pickpointName?: string | null;
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

export interface PickpointOrderItem {
  productName: string;
  skuCode: string;
  sizeLabel: string | null;
  imageUrl: string | null;
  quantity: number;
}

export interface PickpointOrder {
  orderId: number;
  customerFirstName: string;
  customerPhone: string;
  status: OrderStatus;
  readyAt: string;
  expiresAt: string;
  daysUntilExpiry: number;
  overdue: boolean;
  daysOverdue: number;
  totalItemCount: number;
  totalWeightKg: number | null;
  items: PickpointOrderItem[];
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
