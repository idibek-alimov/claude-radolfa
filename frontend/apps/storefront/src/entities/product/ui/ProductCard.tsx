"use client";

import { useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { Crown, ImageOff } from "lucide-react";
import type { ListingVariant } from "@/entities/product";
import { Badge } from "@radolfa/shared/ui/badge";
import { formatPrice } from "@radolfa/shared/lib/format";
import { useTranslations } from "next-intl";

interface ProductCardProps {
  listing: ListingVariant;
}

const LOW_STOCK_THRESHOLD = 5;

export default function ProductCard({ listing }: ProductCardProps) {
  const tc = useTranslations("common");

  const [imageError, setImageError] = useState(false);

  const coverImage = listing.images[0] ?? null;
  const showImage = coverImage != null && !imageError;

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

  const heroPriceClass = hasLoyalty
    ? "text-gold"
    : hasDiscount
    ? "text-rose-600"
    : "text-ink";

  const hasOverlayBadges = listing.winningSource != null || listing.tags.length > 0;

  return (
    <Link
      href={`/products/${listing.productCode}`}
      className="group flex flex-col h-full rounded-[14px] overflow-visible border border-[rgba(14,17,22,0.07)] bg-card shadow-[0_1px_3px_rgba(14,17,22,0.06)] hover:-translate-y-1 hover:shadow-md transition-all duration-150 ease-out"
    >
      {/* Image */}
      <div className="relative w-full aspect-[3/4] overflow-hidden rounded-[14px_14px_10px_10px] bg-[#F2EBDB] flex-shrink-0">
        {showImage ? (
          <Image
            src={coverImage}
            alt={listing.colorDisplayName ?? "Product image"}
            fill
            className="object-cover group-hover:scale-[1.04] transition-transform duration-300"
            unoptimized
            onError={() => setImageError(true)}
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center">
            <ImageOff className="h-8 w-8 text-[#8B7355] opacity-40" />
          </div>
        )}

        {isOutOfStock && (
          <div className="absolute inset-0 bg-black/40 flex items-center justify-center">
            <Badge variant="destructive" className="text-xs">
              {tc("outOfStock")}
            </Badge>
          </div>
        )}

        {/* Bottom-left pill badges — discount/loyalty first, then data tags */}
        {hasOverlayBadges && (
          <div className="absolute bottom-[10px] left-[10px] flex flex-col items-start gap-1">
            {listing.winningSource === "CAMPAIGN" && (
              <span
                className="inline-flex items-center rounded-full px-[9px] py-[3px] text-[10px] font-bold text-white leading-none whitespace-nowrap"
                style={{ backgroundColor: `#${listing.discountColorHex ?? "D11A2A"}` }}
              >
                {listing.discountName ? `${listing.discountName} · ` : ""}−{listing.discountPercentage}%
              </span>
            )}
            {listing.winningSource === "LOYALTY" && listing.loyaltyPercentage != null && (
              <span className="inline-flex items-center gap-[5px] rounded-full px-[9px] py-[3px] text-[10px] font-bold text-white leading-none whitespace-nowrap bg-gold">
                <Crown className="h-[10px] w-[10px]" />
                −{listing.loyaltyPercentage}%
              </span>
            )}
            {listing.tags.slice(0, 2).map((tag) => (
              <span
                key={tag.id}
                className="inline-flex items-center rounded-full px-[9px] py-[3px] text-[10px] font-bold text-white leading-none whitespace-nowrap"
                style={{ backgroundColor: `#${tag.colorHex}` }}
              >
                {tag.name}
              </span>
            ))}
          </div>
        )}
      </div>

      {/* Info */}
      <div className="pt-[11px] px-[13px] pb-[10px] flex flex-col">
        {/* Price row */}
        <div className="flex items-baseline gap-[7px] mb-[5px]">
          <span className={`text-[17px] font-extrabold tabular-nums leading-none ${heroPriceClass}`}>
            {formatPrice(heroPrice)}
          </span>
          {hasCheaperPrice && (
            <span className="text-[12px] font-normal text-gray-400 line-through tabular-nums leading-none">
              {formatPrice(listing.originalPrice)}
            </span>
          )}
        </div>

        {/* Seller / product name */}
        <p className="text-[12px] font-normal text-gray-700 leading-[1.4] mb-[5px] truncate">
          <strong className="uppercase font-bold text-ink">{sellerName}</strong>
          {" / "}
          {listing.colorDisplayName ?? "—"}
        </p>

        {/* Rating */}
        {hasRating && (
          <div className="flex items-center gap-1 text-[11px] text-gray-500">
            <span className="text-amber-500 text-[12px] leading-none">★</span>
            <span className="font-bold text-ink text-[11px]">
              {listing.ratingAverage?.toFixed(1)}
            </span>
            <span>· {listing.reviewCount} reviews</span>
          </div>
        )}

        {isLowStock && (
          <span className="text-[10px] font-medium text-orange-600 mt-1">
            {tc("lowStock", { count: stock })}
          </span>
        )}
      </div>
    </Link>
  );
}
