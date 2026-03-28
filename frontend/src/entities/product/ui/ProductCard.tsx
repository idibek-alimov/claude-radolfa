"use client";

import { useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { motion } from "framer-motion";
import { Crown } from "lucide-react";
import type { ListingVariant } from "@/entities/product";
import { Badge } from "@/shared/ui/badge";
import { formatPrice } from "@/shared/lib/format";
import { useTranslations } from "next-intl";

interface ProductCardProps {
  listing: ListingVariant;
}

const LOW_STOCK_THRESHOLD = 5;

export default function ProductCard({ listing }: ProductCardProps) {
  const tc = useTranslations("common");
  const [isHovered, setIsHovered] = useState(false);

  const coverImage = listing.images[0] ?? null;
  const hoverImage = listing.images[1] ?? null;

  const hasDiscount     = listing.discountPrice != null;
  const hasLoyalty      = listing.loyaltyPrice != null;
  const hasCheaperPrice = hasDiscount || hasLoyalty;
  const stock = listing.skus.reduce((acc, s) => acc + s.stockQuantity, 0);
  const isOutOfStock = stock === 0;
  const isLowStock = stock > 0 && stock <= LOW_STOCK_THRESHOLD;

  return (
    <Link href={`/products/${listing.slug}`} className="group block">
      <motion.div
        whileHover={{ y: -4 }}
        transition={{ duration: 0.2 }}
        className="relative rounded-lg sm:rounded-xl border bg-card text-card-foreground shadow-sm hover:shadow-lg transition-shadow overflow-hidden flex flex-col h-full"
        onMouseEnter={() => setIsHovered(true)}
        onMouseLeave={() => setIsHovered(false)}
      >
        {/* Top badges row */}
        <div className="absolute top-1.5 left-1.5 right-1.5 z-10 sm:top-3 sm:left-3 sm:right-3 flex items-start justify-between gap-1">
          {/* Left: Popular + sale badges stacked */}
          <div className="flex flex-col gap-1">
            {hasDiscount && (
              <Badge
                style={{ backgroundColor: listing.discountColorHex ?? "#ef4444" }}
                className="text-white text-[9px] sm:text-xs shrink-0 border-0"
              >
                {listing.discountName} · -{listing.discountPercentage}%
                {listing.isPartialDiscount && (
                  <span className="font-normal opacity-80"> · select sizes</span>
                )}
              </Badge>
            )}
          </div>

          {/* Right: Color dot + label */}
          {listing.colorKey && (
            <div className="flex items-center gap-1 bg-white/80 backdrop-blur-sm rounded-full px-1.5 sm:px-2 py-0.5 min-w-0">
              {listing.colorHex && (
                <span
                  className="inline-block w-2.5 h-2.5 sm:w-3 sm:h-3 rounded-full border border-black/10 shrink-0"
                  style={{ backgroundColor: listing.colorHex }}
                />
              )}
              <span className="text-[9px] sm:text-xs font-medium text-foreground truncate">
                {listing.colorKey}
              </span>
            </div>
          )}
        </div>

        {/* Cover image with hover swap */}
        <div className="relative w-full aspect-[4/5] sm:aspect-square bg-muted overflow-hidden">
          {coverImage ? (
            <>
              <Image
                src={coverImage}
                alt={listing.colorDisplayName ?? "Product image"}
                fill
                className={`object-cover transition-opacity duration-300 ${
                  isHovered && hoverImage ? "opacity-0" : "opacity-100"
                }`}
                unoptimized
              />
              {hoverImage && (
                <Image
                  src={hoverImage}
                  alt={listing.colorDisplayName ?? "Product image"}
                  fill
                  className={`object-cover transition-opacity duration-300 ${
                    isHovered ? "opacity-100" : "opacity-0"
                  }`}
                  unoptimized
                />
              )}
            </>
          ) : (
            <div className="w-full h-full flex items-center justify-center">
              <span className="text-muted-foreground text-xs sm:text-sm">
                No image
              </span>
            </div>
          )}

          {/* Out of stock overlay */}
          {isOutOfStock && (
            <div className="absolute inset-0 bg-black/40 flex items-center justify-center">
              <Badge variant="destructive" className="text-xs">
                {tc("outOfStock")}
              </Badge>
            </div>
          )}

          {/* Tier price badge */}
          {hasLoyalty && (
            <div className="absolute bottom-1.5 right-1.5 z-10 sm:bottom-3 sm:right-3">
              <span className="inline-flex items-center gap-0.5 rounded-full bg-amber-500 px-1.5 sm:px-2 py-0.5 text-[8px] sm:text-xs font-bold text-white shadow-sm shrink-0">
                <Crown className="h-2.5 w-2.5 sm:h-3 sm:w-3" />
                {tc("yourPrice")}
              </span>
            </div>
          )}
        </div>

        {/* Body */}
        <div className="p-2 sm:p-3 flex flex-col flex-1 gap-0.5 sm:gap-1">
          {/* Category */}
          {listing.categoryName && (
            <span className="text-[10px] sm:text-xs text-muted-foreground uppercase tracking-wide">
              {listing.categoryName}
            </span>
          )}

          <h3 className="font-semibold text-foreground text-sm sm:text-base truncate leading-tight">
            {listing.colorDisplayName ?? "—"}
          </h3>

          {/* Price section */}
          <div className="mt-auto pt-1 sm:pt-1.5 flex flex-col gap-0.5">

            {/* Hero price row */}
            <div className="flex items-center justify-between gap-1">
              <div className="flex items-baseline gap-1 sm:gap-1.5 min-w-0">
                {hasLoyalty ? (
                  <>
                    <Crown className="h-3 w-3 sm:h-4 sm:w-4 text-amber-500 shrink-0" />
                    <span className="text-sm sm:text-lg font-bold text-amber-600 truncate">
                      {formatPrice(listing.loyaltyPrice!)}
                    </span>
                    <span className="hidden sm:inline text-xs font-medium text-amber-600/70">
                      {tc("yourPrice")}
                    </span>
                  </>
                ) : hasDiscount ? (
                  <span className="text-base sm:text-lg font-bold text-red-600">
                    {formatPrice(listing.discountPrice!)}
                  </span>
                ) : (
                  <span className="text-base sm:text-lg font-bold text-violet-600">
                    {formatPrice(listing.originalPrice)}
                  </span>
                )}
              </div>
              {isLowStock && (
                <span className="text-[10px] sm:text-xs font-medium text-orange-600 whitespace-nowrap shrink-0">
                  {tc("lowStock", { count: stock })}
                </span>
              )}
            </div>

            {/* Strikethrough row — only when a cheaper price exists */}
            {hasCheaperPrice && (
              <div className="flex items-baseline gap-1">
                <span className="text-[10px] sm:text-sm text-muted-foreground line-through">
                  {formatPrice(listing.originalPrice)}
                </span>
                {hasDiscount && (
                  <span
                    className="text-[9px] sm:text-xs font-semibold px-1 rounded text-white"
                    style={{ backgroundColor: listing.discountColorHex ?? "#ef4444" }}
                  >
                    -{listing.discountPercentage}%
                  </span>
                )}
              </div>
            )}

            {/* Loyalty tier label */}
            {hasLoyalty && listing.loyaltyPercentage != null && (
              <span className="text-[9px] sm:text-xs text-amber-600 font-medium">
                {tc("loyaltyTierBadge", { pct: listing.loyaltyPercentage })}
              </span>
            )}

          </div>
        </div>
      </motion.div>
    </Link>
  );
}
