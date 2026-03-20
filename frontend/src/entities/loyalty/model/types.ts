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
}
