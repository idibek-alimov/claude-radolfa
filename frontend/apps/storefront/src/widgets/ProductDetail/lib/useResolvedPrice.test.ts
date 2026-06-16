import { describe, expect, it } from "vitest";
import type { ListingVariantDetail, Sku } from "@/entities/product";
import { useResolvedPrice } from "./useResolvedPrice";

/** Minimal but fully-typed listing fixture — only pricing-relevant fields vary per test. */
function makeListing(overrides: Partial<ListingVariantDetail> = {}): ListingVariantDetail {
  return {
    productBaseId: 1,
    variantId: 1,
    productCode: "PC-1",
    slug: "product-1",
    colorKey: "cognac",
    colorDisplayName: "Cognac",
    colorHex: "#8B5A2B",
    categoryName: "Bags",
    webDescription: null,
    images: [],
    tags: [],
    skus: [],
    originalPrice: 460,
    discountPrice: null,
    discountPercentage: null,
    discountName: null,
    discountColorHex: null,
    loyaltyPrice: null,
    loyaltyPercentage: null,
    winningSource: null,
    isPartialDiscount: false,
    ratingAverage: null,
    reviewCount: 0,
    sellerShopName: null,
    brandId: null,
    brandName: null,
    categoryId: null,
    weightKg: null,
    widthCm: null,
    heightCm: null,
    depthCm: null,
    attributes: [],
    siblingVariants: [],
    reviewTraits: [],
    sellerId: null,
    colorName: null,
    categorySlug: null,
    ...overrides,
  };
}

function makeSku(overrides: Partial<Sku> = {}): Sku {
  return {
    skuId: 1,
    skuCode: "SKU-1",
    sizeLabel: "M",
    stockQuantity: 5,
    originalPrice: 460,
    discountPrice: null,
    discountPercentage: null,
    discountName: null,
    discountColorHex: null,
    loyaltyPrice: null,
    winningSource: null,
    ...overrides,
  };
}

describe("useResolvedPrice", () => {
  it("falls back to variant pricing when no SKU is selected and there is no sale", () => {
    const listing = makeListing();
    const result = useResolvedPrice(listing, null);

    expect(result.originalPrice).toBe(460);
    expect(result.hasDiscount).toBe(false);
    expect(result.hasLoyalty).toBe(false);
    expect(result.hasCheaperPrice).toBe(false);
    expect(result.saveAmount).toBeNull();
    expect(result.effectivePrice).toBe(460);
    expect(result.winningSource).toBeNull();
  });

  it("resolves a variant-level sale", () => {
    const listing = makeListing({
      discountPrice: 320,
      discountPercentage: 30,
      discountName: "Winter Sale",
      discountColorHex: "ef4444",
      winningSource: "CAMPAIGN",
    });
    const result = useResolvedPrice(listing, null);

    expect(result.hasDiscount).toBe(true);
    expect(result.hasLoyalty).toBe(false);
    expect(result.hasCheaperPrice).toBe(true);
    expect(result.effectivePrice).toBe(320);
    expect(result.saveAmount).toBe(140);
    expect(result.winningSource).toBe("CAMPAIGN");
  });

  it("resolves loyalty pricing and reads loyaltyPercentage from the variant", () => {
    const listing = makeListing({
      loyaltyPrice: 281,
      loyaltyPercentage: 12,
      discountPrice: 320,
      discountPercentage: 30,
      discountName: "Winter Sale",
      discountColorHex: "ef4444",
      winningSource: "LOYALTY",
    });
    const result = useResolvedPrice(listing, null);

    expect(result.hasLoyalty).toBe(true);
    expect(result.loyaltyPercentage).toBe(12);
    // Loyalty wins over the campaign discount for the hero/effective price.
    expect(result.effectivePrice).toBe(281);
    expect(result.saveAmount).toBe(460 - 281);
    expect(result.winningSource).toBe("LOYALTY");
  });

  it("overrides variant pricing with the selected SKU's own pricing block", () => {
    const listing = makeListing({
      discountPrice: 320,
      discountPercentage: 30,
      discountName: "Winter Sale",
      discountColorHex: "ef4444",
      winningSource: "CAMPAIGN",
      isPartialDiscount: true,
    });
    const sku = makeSku({
      originalPrice: 500,
      discountPrice: 450,
      discountPercentage: 10,
      discountName: "Size M Sale",
      discountColorHex: "10b981",
      winningSource: "CAMPAIGN",
    });
    const result = useResolvedPrice(listing, sku);

    expect(result.originalPrice).toBe(500);
    expect(result.discountPrice).toBe(450);
    expect(result.discountName).toBe("Size M Sale");
    expect(result.effectivePrice).toBe(450);
    expect(result.saveAmount).toBe(50);
  });

  it("falls back to the variant's discount when the selected SKU has discountPrice: null (?? semantics)", () => {
    const listing = makeListing({
      discountPrice: 320,
      discountPercentage: 30,
      discountName: "Winter Sale",
      discountColorHex: "ef4444",
      winningSource: "CAMPAIGN",
      isPartialDiscount: true,
    });
    const sku = makeSku({ discountPrice: null, discountPercentage: null, winningSource: null });
    const result = useResolvedPrice(listing, sku);

    // `??` falls through on `null`, so a SKU with no sale of its own inherits
    // the variant-level discount — this is the existing extracted behaviour.
    expect(result.hasDiscount).toBe(true);
    expect(result.discountPrice).toBe(320);
    expect(result.winningSource).toBe("CAMPAIGN");
  });

  it("falls back to the variant's loyaltyPrice when the selected SKU has none", () => {
    const listing = makeListing({ loyaltyPrice: 281, loyaltyPercentage: 12 });
    const sku = makeSku({ loyaltyPrice: null });
    const result = useResolvedPrice(listing, sku);

    expect(result.hasLoyalty).toBe(true);
    expect(result.loyaltyPrice).toBe(281);
  });
});
