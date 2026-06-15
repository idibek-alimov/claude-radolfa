import type { ListingVariantDetail, Sku } from "@/entities/product";

/**
 * Resolved pricing for the buy box — selected SKU pricing overrides the
 * variant-level pricing block field-by-field via `??`. This mirrors the
 * fallback the PDP monolith always used (selecting a SKU swaps to that
 * SKU's own pricing block), plus two fields the monolith never read:
 * `loyaltyPercentage` (variant-only — `Sku` carries no per-SKU tier %) and
 * `winningSource` (SKU `??` variant), which the redesign's "Crown price"
 * row uses to label which mechanism produced the price.
 */
export interface ResolvedPrice {
  originalPrice: number;
  discountPrice: number | null;
  discountPercentage: number | null;
  discountName: string | null;
  discountColorHex: string | null;
  loyaltyPrice: number | null;
  loyaltyPercentage: number | null;
  winningSource: "CAMPAIGN" | "LOYALTY" | null;
  hasDiscount: boolean;
  hasLoyalty: boolean;
  hasCheaperPrice: boolean;
  /** originalPrice − effective price (loyalty ?? discount), for the SAVE badge. Null if no cheaper price. */
  saveAmount: number | null;
  /** The price actually shown as the hero number — loyalty if present, else discount, else original. */
  effectivePrice: number;
}

/**
 * Resolve the buy-box pricing block, falling back from the selected SKU to
 * the listing variant. Pure function — no hooks, no side effects.
 */
export function useResolvedPrice(
  listing: ListingVariantDetail,
  selectedSku: Sku | null,
): ResolvedPrice {
  const originalPrice = selectedSku?.originalPrice ?? listing.originalPrice;
  const discountPrice = selectedSku?.discountPrice ?? listing.discountPrice;
  const discountPercentage = selectedSku?.discountPercentage ?? listing.discountPercentage;
  const discountName = selectedSku?.discountName ?? listing.discountName;
  const discountColorHex = selectedSku?.discountColorHex ?? listing.discountColorHex;
  const loyaltyPrice = selectedSku?.loyaltyPrice ?? listing.loyaltyPrice;
  // `Sku` carries no per-SKU loyalty percentage — always read from the variant.
  const loyaltyPercentage = listing.loyaltyPercentage;
  const winningSource = selectedSku?.winningSource ?? listing.winningSource;

  const hasDiscount = discountPrice != null;
  const hasLoyalty = loyaltyPrice != null;
  const hasCheaperPrice = hasDiscount || hasLoyalty;

  const effectivePrice = loyaltyPrice ?? discountPrice ?? originalPrice;
  const saveAmount = hasCheaperPrice ? originalPrice - effectivePrice : null;

  return {
    originalPrice,
    discountPrice,
    discountPercentage,
    discountName,
    discountColorHex,
    loyaltyPrice,
    loyaltyPercentage,
    winningSource,
    hasDiscount,
    hasLoyalty,
    hasCheaperPrice,
    saveAmount,
    effectivePrice,
  };
}
