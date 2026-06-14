// ── Public API of the loyalty entity slice ───────────────────────
export type { LoyaltyTier, LoyaltyProfile } from "./model/types";
export { useLoyaltyTiers, updateTierColor } from "./api";
export { default as ProfileLoyaltyHero } from "./ui/ProfileLoyaltyHero";
