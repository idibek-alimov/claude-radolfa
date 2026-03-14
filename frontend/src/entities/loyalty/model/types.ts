/**
 * Loyalty tier definition synced from ERPNext.
 * Returned by GET /api/v1/loyalty-tiers.
 */
export interface LoyaltyTier {
  name: string;
  discountPercentage: number;
  cashbackPercentage: number;
  minSpendRequirement: number;
  /** Lower = higher tier (1 is best). */
  displayOrder: number;
}

/**
 * User's loyalty profile, nested inside the User object.
 * Returned as part of GET /api/v1/auth/me.
 */
export interface LoyaltyProfile {
  points: number;
  tier: LoyaltyTier | null;
  spendToNextTier: number | null;
  spendToMaintainTier: number | null;
  currentMonthSpending: number | null;
}
