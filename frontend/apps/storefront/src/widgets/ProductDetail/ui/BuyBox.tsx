"use client";

import Image from "next/image";
import { useTranslations } from "next-intl";
import { Crown, Zap } from "lucide-react";
import { toast } from "sonner";
import { useQueryClient } from "@tanstack/react-query";
import { getErrorMessage } from "@radolfa/shared/lib";
import { formatPrice } from "@radolfa/shared/lib/format";
import type { ListingVariantDetail, Sku } from "@/entities/product";
import { StockBadge, fetchListingBySlug } from "@/entities/product";
import { useAddToCart } from "@/features/cart";
import { useResolvedPrice } from "../lib/useResolvedPrice";

const heroPriceFormatter = new Intl.NumberFormat("en-US", { maximumFractionDigits: 0 });

/** Bare integer with space-separated thousands, no currency suffix — for the hero price number. */
function formatHeroPrice(price: number): string {
  return heroPriceFormatter.format(price).replace(/,/g, " ");
}

/** "midnight-black" → "Midnight Black" — fallback when colorName is null. */
function formatColorKey(key: string): string {
  return key
    .split("-")
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(" ");
}

interface BuyBoxProps {
  listing: ListingVariantDetail;
  /** The currently active color slug — drives the active-swatch highlight. */
  activeSlug: string;
  selectedSku: Sku | null;
  onSelectSku: (sku: Sku | null) => void;
  /** Called when the user clicks a color swatch; triggers an in-place data swap. */
  onSelectColor: (slug: string) => void;
}

export default function BuyBox({ listing, activeSlug, selectedSku, onSelectSku, onSelectColor }: BuyBoxProps) {
  const t = useTranslations("productDetail");
  const addToCart = useAddToCart();
  const price = useResolvedPrice(listing, selectedSku);
  const queryClient = useQueryClient();

  /** Warm the cache for a sibling color on hover/focus so the click is instant. */
  const prefetchSibling = (slug: string) => {
    void queryClient.prefetchQuery({
      queryKey: ["listing", slug],
      queryFn: () => fetchListingBySlug(slug),
    });
  };

  const totalStock = listing.skus.reduce((acc, s) => acc + s.stockQuantity, 0);
  const allOutOfStock = totalStock === 0;
  const isLowStock =
    selectedSku != null && selectedSku.stockQuantity > 0 && selectedSku.stockQuantity <= 5;

  const ctaDisabled = !selectedSku || selectedSku.stockQuantity === 0 || addToCart.isPending;

  const handleAddToCart = () => {
    if (!selectedSku) return;
    addToCart.mutate(
      { skuId: selectedSku.skuId, quantity: 1 },
      { onError: (err) => toast.error(getErrorMessage(err)) },
    );
  };

  return (
    <div className="lg:rounded-3xl lg:bg-white lg:border-2 lg:border-mag/15 lg:p-7 lg:shadow-[0_8px_30px_-12px_rgba(203,17,171,0.25)]">
      {/* ── Seller line ─────────────────────────────────────────── */}
      <div className="text-[12px] text-ink font-medium mb-1.5">
        {listing.sellerShopName ?? "Radolfa"}
        {listing.sellerShopName && (
          <span className="ml-1.5 text-emerald font-bold">· {t("verifiedSeller")}</span>
        )}
      </div>

      {/* ── Title + subtitle ────────────────────────────────────── */}
      <h1 className="text-[22px] lg:text-[28px] font-black leading-tight">
        {listing.colorDisplayName}
      </h1>

      {/* ── Rating row ──────────────────────────────────────────── */}
      {listing.reviewCount > 0 && (
        <div className="mt-3 flex items-center gap-2 text-[12px]">
          <span className="px-2 py-0.5 rounded-md bg-gold/20 text-[#9A6E0F] font-bold inline-flex items-center gap-1">
            ★ {(listing.ratingAverage ?? 0).toFixed(1)}
          </span>
          <a href="#reviews" className="text-mag hover:underline">
            {t("reviewsCount", { count: listing.reviewCount })}
          </a>
        </div>
      )}

      <hr className="my-5 border-ink/8" />

      {/* ── Price block ─────────────────────────────────────────── */}
      <div className="flex items-baseline gap-3 flex-wrap">
        <span className="text-[34px] lg:text-[44px] font-black text-mag tabular-nums leading-none">
          {formatHeroPrice(price.effectivePrice)}
        </span>
        <span className="text-[14px] font-bold text-mag">TJS</span>
        {price.hasCheaperPrice && (
          <span className="text-[18px] text-ink/45 line-through tabular-nums">
            {formatPrice(price.originalPrice)}
          </span>
        )}
        {price.saveAmount != null && price.saveAmount > 0 && (
          <span className="ml-auto px-2.5 py-1 rounded-md bg-sale text-white text-[11px] font-bold">
            {t("save", { amount: formatPrice(price.saveAmount) })}
          </span>
        )}
      </div>

      {/* Crown / loyalty row */}
      {price.loyaltyPrice != null && (
        <div className="mt-2 px-3 py-2 rounded-xl bg-gold/15 text-[12px] flex items-center gap-2 w-full">
          <Crown className="h-[13px] w-[13px] shrink-0" style={{ color: "#9A6E0F" }} />
          <span>
            <strong style={{ color: "#9A6E0F" }}>
              {t("crownPrice", { price: formatPrice(price.loyaltyPrice) })}
            </strong>
            {price.loyaltyPercentage != null &&
              ` — ${t("crownPercentageOff", { pct: price.loyaltyPercentage })}`}
          </span>
        </div>
      )}

      {/* Partial discount hint — shown at variant level before a size is selected */}
      {listing.isPartialDiscount && !selectedSku && (
        <p className="mt-2 text-[12px] text-ink/55">{t("partialDiscountHint")}</p>
      )}

      {/* ── Colour picker ───────────────────────────────────────── */}
      {(() => {
        const swatches = [
          {
            slug: listing.slug,
            colorKey: listing.colorKey,
            colorHex: listing.colorHex,
            thumbnail: listing.images[0] ?? null,
          },
          ...listing.siblingVariants,
        ].sort(
          (a, b) =>
            a.colorKey.localeCompare(b.colorKey) || a.slug.localeCompare(b.slug),
        );

        return (
          <div className="mt-5">
            <div className="text-[12px] mb-2">
              <span className="text-ink/55">{t("color")}:</span>{" "}
              <span className="font-bold">
                {listing.colorName ?? formatColorKey(listing.colorKey)}
              </span>
              {listing.siblingVariants.length > 0 &&
                ` · ${t("coloursAvailable", { count: swatches.length })}`}
            </div>
            <div className="flex gap-2.5 flex-wrap">
              {swatches.map((sv) => {
                // Compare against activeSlug (not listing.slug) for immediate
                // visual feedback on click before the new listing data arrives.
                const isActive = sv.slug === activeSlug;
                return isActive ? (
                  <span
                    key={sv.slug}
                    role="button"
                    aria-pressed="true"
                    aria-label={listing.colorName ?? sv.colorKey}
                    className="w-14 h-14 rounded-2xl border-2 border-mag p-0.5 overflow-hidden ring-2 ring-mag/20 relative block shrink-0"
                    style={{ backgroundColor: sv.colorHex ?? undefined }}
                  >
                    {sv.thumbnail && (
                      <Image
                        src={sv.thumbnail}
                        alt={sv.colorKey}
                        fill
                        className="object-cover rounded-xl"
                        unoptimized
                      />
                    )}
                  </span>
                ) : (
                  // Plain <a> keeps the href for crawlers, middle-click, and
                  // "open in new tab". A normal left-click is intercepted and
                  // swaps the data in place without a router navigation.
                  <a
                    key={sv.slug}
                    href={`/products/${sv.slug}`}
                    aria-label={sv.colorKey}
                    className="w-14 h-14 rounded-2xl border border-ink/15 overflow-hidden hover:border-mag relative block shrink-0"
                    style={{ backgroundColor: sv.colorHex ?? undefined }}
                    onMouseEnter={() => prefetchSibling(sv.slug)}
                    onFocus={() => prefetchSibling(sv.slug)}
                    onClick={(e) => {
                      // Let modified clicks (new tab, new window) go through normally.
                      if (e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return;
                      e.preventDefault();
                      onSelectColor(sv.slug);
                    }}
                  >
                    {sv.thumbnail && (
                      <Image
                        src={sv.thumbnail}
                        alt={sv.colorKey}
                        fill
                        className="object-cover"
                        unoptimized
                      />
                    )}
                  </a>
                );
              })}
            </div>
          </div>
        );
      })()}

      {/* ── Size picker ─────────────────────────────────────────── */}
      {listing.skus.length > 0 && (
        <div className="mt-5">
          <div className="text-[12px] mb-2">
            <span className="text-ink/55">{t("availableSizes")}:</span>{" "}
            {selectedSku && <span className="font-bold">{selectedSku.sizeLabel}</span>}
          </div>
          <div className="grid grid-cols-3 gap-2">
            {listing.skus.map((sku) => {
              const isOutOfStock = sku.stockQuantity === 0;
              const isSelected = selectedSku?.skuId === sku.skuId;

              return (
                <button
                  key={sku.skuId}
                  type="button"
                  disabled={isOutOfStock}
                  aria-pressed={isSelected}
                  onClick={() => onSelectSku(isSelected ? null : sku)}
                  className={`h-11 rounded-xl text-[13px] font-semibold relative transition-colors ${
                    isSelected
                      ? "border-2 border-mag bg-mag/5 text-mag font-bold"
                      : isOutOfStock
                        ? "border border-ink/10 text-ink/30 cursor-not-allowed"
                        : "border border-ink/15 hover:border-mag"
                  }`}
                >
                  {sku.sizeLabel}
                  {isOutOfStock && (
                    <span className="absolute inset-0 flex items-center justify-center">
                      <span className="block w-[calc(100%-12px)] h-px bg-ink/20 rotate-[-20deg]" />
                    </span>
                  )}
                </button>
              );
            })}
          </div>
        </div>
      )}

      {/* ── Urgency ─────────────────────────────────────────────── */}
      {isLowStock && (
        <div className="mt-4 px-3 py-2 rounded-xl bg-sale/8 text-[12px] text-sale font-semibold inline-flex items-center gap-2">
          <Zap className="h-[13px] w-[13px] shrink-0" />
          {t("itemsLeft", { count: selectedSku!.stockQuantity })}
        </div>
      )}

      {/* ── CTA ─────────────────────────────────────────────────── */}
      <div className="mt-5">
        {allOutOfStock ? (
          <div className="space-y-2">
            <StockBadge stock={0} />
            <button
              type="button"
              disabled
              className="w-full h-14 rounded-full bg-mag/30 text-white font-black text-[16px] cursor-not-allowed"
            >
              {t("outOfStock")}
            </button>
          </div>
        ) : (
          <button
            type="button"
            onClick={handleAddToCart}
            disabled={ctaDisabled}
            className="w-full h-14 rounded-full bg-mag text-white font-black text-[16px] hover:bg-maglo disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {!selectedSku
              ? t("selectSize")
              : addToCart.isPending
                ? t("adding")
                : t("addToBag", { price: formatPrice(price.effectivePrice) })}
          </button>
        )}
      </div>
    </div>
  );
}
