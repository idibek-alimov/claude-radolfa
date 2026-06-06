"use client";

import Image from "next/image";
import Link from "next/link";
import { motion } from "framer-motion";
import { Crown, ShoppingCart } from "lucide-react";
import type { ListingVariant } from "@/entities/product";
import { TagBadge } from "@/entities/tag";
import { Badge } from "@radolfa/shared/ui/badge";
import { Button } from "@radolfa/shared/ui/button";
import { formatPrice } from "@radolfa/shared/lib/format";
import { useTranslations } from "next-intl";

interface ProductCardProps {
  listing: ListingVariant;
  /** "home" = homepage 5-up / 2-up rows.
   *  "grid" = catalog / category grid (default). */
  variant?: "home" | "grid";
}

const LOW_STOCK_THRESHOLD = 5;

export default function ProductCard({
  listing,
  variant = "grid",
}: ProductCardProps) {
  const tc = useTranslations("common");

  const coverImage = listing.images[0] ?? null;

  const hasDiscount = listing.discountPrice != null;
  const hasLoyalty  = listing.loyaltyPrice != null;
  const hasCheaperPrice = hasDiscount || hasLoyalty;
  const stock = listing.skus.reduce((acc, s) => acc + s.stockQuantity, 0);
  const isOutOfStock = stock === 0;
  const isLowStock   = stock > 0 && stock <= LOW_STOCK_THRESHOLD;
  const hasRating    = listing.reviewCount > 0;

  const heroPrice = hasLoyalty
    ? listing.loyaltyPrice!
    : hasDiscount
    ? listing.discountPrice!
    : listing.originalPrice;

  const sellerName = listing.sellerShopName ?? "Radolfa";

  // ── Shared: image overlay badges ─────────────────────────────────────────
  const DiscountBadge = hasDiscount ? (
    <span
      className="inline-flex items-center px-1.5 py-0.5 rounded text-white text-[10px] font-bold leading-none"
      style={{ backgroundColor: listing.discountColorHex ?? "#ef4444" }}
    >
      -{listing.discountPercentage}%
    </span>
  ) : hasLoyalty && listing.loyaltyPercentage != null ? (
    <span className="inline-flex items-center gap-0.5 px-1.5 py-0.5 rounded bg-amber-500 text-white text-[10px] font-bold leading-none">
      <Crown className="h-2.5 w-2.5" />
      -{listing.loyaltyPercentage}%
    </span>
  ) : null;

  const TagBadges =
    listing.tags.length > 0 ? (
      <div className="flex flex-col gap-0.5 mt-0.5">
        {listing.tags.slice(0, 2).map((tag) => (
          <TagBadge key={tag.id} tag={tag} size="sm" />
        ))}
      </div>
    ) : null;

  const heroPriceClass = hasLoyalty
    ? "text-amber-500"
    : hasDiscount
    ? "text-rose-600"
    : "text-foreground";

  // ── HOME variant ─────────────────────────────────────────────────────────
  if (variant === "home") {
    return (
      <div className="group flex flex-col h-full rounded-xl border bg-card shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all duration-150 overflow-hidden">
        <Link href={`/products/${listing.slug}`} className="block flex-1 flex flex-col">
          {/* Image */}
          <div className="relative w-full aspect-[3/4] bg-muted rounded-t-xl overflow-hidden">
            {coverImage ? (
              <Image
                src={coverImage}
                alt={listing.colorDisplayName ?? "Product image"}
                fill
                className="object-cover group-hover:scale-105 transition-transform duration-300"
                unoptimized
              />
            ) : (
              <div className="w-full h-full flex items-center justify-center">
                <span className="text-muted-foreground text-xs">No image</span>
              </div>
            )}

            {isOutOfStock && (
              <div className="absolute inset-0 bg-black/40 flex items-center justify-center">
                <Badge variant="destructive" className="text-xs">
                  {tc("outOfStock")}
                </Badge>
              </div>
            )}

            {/* Bottom-left: discount + tags stacked */}
            {(DiscountBadge || TagBadges) && (
              <div className="absolute bottom-2 left-2 flex flex-col gap-0.5">
                {DiscountBadge}
                {TagBadges}
              </div>
            )}
          </div>

          {/* Info */}
          <div className="p-2 flex flex-col gap-0.5 flex-1">
            {/* Price row */}
            <div className="flex items-baseline gap-1.5 flex-wrap">
              {hasLoyalty && (
                <Crown className="h-3 w-3 text-amber-500 shrink-0 self-center" />
              )}
              <span className={`text-sm font-bold tabular-nums leading-none ${heroPriceClass}`}>
                {formatPrice(heroPrice)}
              </span>
              {hasCheaperPrice && (
                <span className="text-[10px] text-muted-foreground line-through tabular-nums">
                  {formatPrice(listing.originalPrice)}
                </span>
              )}
            </div>

            {/* Seller / name */}
            <p className="text-[11px] leading-tight truncate">
              <span className="text-muted-foreground font-medium">{sellerName}</span>
              <span className="text-muted-foreground"> / </span>
              <span className="text-foreground font-semibold">{listing.colorDisplayName ?? "—"}</span>
            </p>

            {/* Rating */}
            {hasRating && (
              <span className="text-[10px] text-muted-foreground">
                ★ {listing.ratingAverage?.toFixed(1)} · {listing.reviewCount}
              </span>
            )}

            {isLowStock && (
              <span className="text-[10px] font-medium text-orange-600">
                {tc("lowStock", { count: stock })}
              </span>
            )}
          </div>
        </Link>

        {/* Add to Cart */}
        <div className="px-2 pb-2">
          <Link href={`/products/${listing.slug}`} className="block w-full">
            <Button
              size="sm"
              className="w-full gap-1.5 text-[11px] h-7"
              disabled={isOutOfStock}
            >
              <ShoppingCart className="h-3 w-3" />
              {isOutOfStock ? tc("outOfStock") : tc("addToCart")}
            </Button>
          </Link>
        </div>
      </div>
    );
  }

  // ── GRID variant ─────────────────────────────────────────────────────────
  return (
    <motion.div
      whileHover={{ y: -4 }}
      transition={{ duration: 0.15 }}
      className="group flex flex-col h-full rounded-xl border bg-card shadow-sm hover:shadow-md transition-shadow overflow-hidden"
    >
      <Link href={`/products/${listing.slug}`} className="block flex-1 flex flex-col">
        {/* Image */}
        <div className="relative w-full aspect-[3/4] bg-muted rounded-t-xl overflow-hidden">
          {coverImage ? (
            <Image
              src={coverImage}
              alt={listing.colorDisplayName ?? "Product image"}
              fill
              className="object-cover group-hover:scale-105 transition-transform duration-300"
              unoptimized
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center">
              <span className="text-muted-foreground text-sm">No image</span>
            </div>
          )}

          {isOutOfStock && (
            <div className="absolute inset-0 bg-black/40 flex items-center justify-center">
              <Badge variant="destructive">{tc("outOfStock")}</Badge>
            </div>
          )}

          {/* Bottom-left overlay: discount badge + tag badges */}
          {(DiscountBadge || TagBadges) && (
            <div className="absolute bottom-2.5 left-2.5 flex flex-col gap-1">
              {DiscountBadge}
              {TagBadges}
            </div>
          )}
        </div>

        {/* Info */}
        <div className="p-3 flex flex-col gap-1 flex-1">
          {/* Price row */}
          <div className="flex items-baseline gap-2 flex-wrap">
            {hasLoyalty && (
              <Crown className="h-3.5 w-3.5 text-amber-500 shrink-0 self-center" />
            )}
            <span className={`text-base sm:text-lg font-bold tabular-nums leading-none ${heroPriceClass}`}>
              {formatPrice(heroPrice)}
            </span>
            {hasCheaperPrice && (
              <span className="text-xs sm:text-sm text-muted-foreground line-through tabular-nums">
                {formatPrice(listing.originalPrice)}
              </span>
            )}
          </div>

          {/* Seller / product name */}
          <p className="text-sm leading-snug line-clamp-2">
            <span className="text-muted-foreground font-medium">{sellerName}</span>
            <span className="text-muted-foreground"> / </span>
            <span className="text-foreground font-semibold">{listing.colorDisplayName ?? "—"}</span>
          </p>

          {/* Rating */}
          {hasRating && (
            <span className="text-xs text-muted-foreground mt-auto">
              ★ {listing.ratingAverage?.toFixed(1)} · {listing.reviewCount}
            </span>
          )}

          {isLowStock && (
            <span className="text-xs font-medium text-orange-600">
              {tc("lowStock", { count: stock })}
            </span>
          )}
        </div>
      </Link>

      {/* Add to Cart */}
      <div className="px-3 pb-3">
        <Link href={`/products/${listing.slug}`} className="block w-full">
          <Button
            className="w-full gap-2"
            size="sm"
            disabled={isOutOfStock}
          >
            <ShoppingCart className="h-4 w-4" />
            {isOutOfStock ? tc("outOfStock") : tc("addToCart")}
          </Button>
        </Link>
      </div>
    </motion.div>
  );
}
