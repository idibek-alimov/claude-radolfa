// Synced from backend LoyaltyReason enum — do not edit manually
export type LoyaltyReason =
  | "EARN_CASHBACK"
  | "REVIEW_BONUS"
  | "REDEEM"
  | "RESTORE"
  | "REVOKE"
  | "EXPIRE"
  | "MANUAL_ADJUSTMENT"
  | "OPENING_BALANCE";

// Synced from backend LoyaltyLedgerDto — do not edit manually
export interface LoyaltyLedgerEntry {
  id: number;
  userId: number;
  delta: number;
  reason: LoyaltyReason;
  orderId: number | null;
  actorUserId: number | null;
  note: string | null;
  remainingPoints: number | null;
  expiresAt: string | null;   // ISO instant, nullable (debit rows + never-expire credits)
  balanceAfter: number;
  createdAt: string;           // ISO instant
}

/**
 * Loyalty tier definition.
 * Returned by GET /api/v1/loyalty-tiers.
 */
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

/**
 * A single loyalty point earning record.
 * Embedded in LoyaltyProfile.recentEarnings[].
 */
export interface LoyaltyEarning {
  orderId: number;
  pointsEarned: number;
  orderAmount: number;
  orderedAt: string;  // ISO-8601
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
