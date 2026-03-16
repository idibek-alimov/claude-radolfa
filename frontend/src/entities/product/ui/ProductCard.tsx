"use client";

import { useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { motion } from "framer-motion";
import { Flame, Crown } from "lucide-react";
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

  const hasDiscount = listing.discountedPrice != null;
  const hasLoyalty = listing.loyaltyPrice != null;
  const stock = listing.totalStock ?? 0;
  const isOutOfStock = stock === 0;
  const isLowStock = stock > 0 && stock <= LOW_STOCK_THRESHOLD;

  const hasSaleBorder = hasDiscount && listing.saleColorHex;

  return (
    <Link href={`/products/${listing.slug}`} className="group block">
      <motion.div
        whileHover={{ y: -4 }}
        transition={{ duration: 0.2 }}
        className="relative rounded-lg sm:rounded-xl bg-card text-card-foreground shadow-sm hover:shadow-lg transition-shadow overflow-hidden flex flex-col h-full"
        style={
          hasSaleBorder
            ? {
                border: `2px solid ${listing.saleColorHex}`,
                boxShadow: isHovered
                  ? `0 8px 25px ${listing.saleColorHex}30`
                  : `0 0 0 0 transparent`,
              }
            : { border: "1px solid var(--border)" }
        }
        onMouseEnter={() => setIsHovered(true)}
        onMouseLeave={() => setIsHovered(false)}
      >
        {/* Top-left: Popular badge */}
        {listing.topSelling && (
          <Badge
            variant="default"
            className="absolute top-2 left-2 z-10 text-[10px] sm:text-xs sm:top-3 sm:left-3 gap-0.5"
          >
            <Flame className="h-3 w-3" />
            {tc("popular")}
          </Badge>
        )}

        {/* Top-right: Color dot + label */}
        {listing.colorKey && (
          <div className="absolute top-2 right-2 z-10 sm:top-3 sm:right-3 flex items-center gap-1.5 bg-white/80 backdrop-blur-sm rounded-full px-2 py-0.5">
            {listing.colorHexCode && (
              <span
                className="inline-block w-3 h-3 rounded-full border border-black/10 shrink-0"
                style={{ backgroundColor: listing.colorHexCode }}
              />
            )}
            <span className="text-[10px] sm:text-xs font-medium text-foreground">
              {listing.colorKey}
            </span>
          </div>
        )}

        {/* Cover image with hover swap */}
        <div className="relative w-full aspect-[4/5] sm:aspect-square bg-muted overflow-hidden">
          {coverImage ? (
            <>
              <Image
                src={coverImage}
                alt={listing.name ?? "Product image"}
                fill
                className={`object-cover transition-opacity duration-300 ${
                  isHovered && hoverImage ? "opacity-0" : "opacity-100"
                }`}
                unoptimized
              />
              {hoverImage && (
                <Image
                  src={hoverImage}
                  alt={listing.name ?? "Product image"}
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

          {/* Bottom-left: Discount % pill + Sale title badge */}
          {listing.discountPercentage != null && (
            <div className="absolute bottom-2 left-2 z-10 sm:bottom-3 sm:left-3 flex flex-col items-start gap-1">
              <span className="inline-flex items-center rounded-full bg-red-500 px-2 py-0.5 text-[10px] sm:text-xs font-bold text-white shadow-sm">
                -{listing.discountPercentage}%
              </span>
              {listing.saleTitle && (
                <span
                  className="inline-flex items-center rounded-md px-2 py-0.5 text-[9px] sm:text-[11px] font-bold uppercase tracking-wide text-white shadow-sm"
                  style={{
                    backgroundColor: listing.saleColorHex ?? "#d946ef",
                  }}
                >
                  {listing.saleTitle}
                </span>
              )}
            </div>
          )}

          {/* Bottom-right: Loyalty badge */}
          {hasLoyalty && listing.loyaltyDiscountPercentage != null && (
            <div className="absolute bottom-2 right-2 z-10 sm:bottom-3 sm:right-3">
              <span className="inline-flex items-center gap-0.5 rounded-full bg-amber-500 px-2 py-0.5 text-[10px] sm:text-xs font-bold text-white shadow-sm">
                <Crown className="h-3 w-3" />
                {tc("loyaltyTag", { pct: listing.loyaltyDiscountPercentage })}
              </span>
            </div>
          )}
        </div>

        {/* Body */}
        <div className="p-2 sm:p-3 flex flex-col flex-1 gap-0.5 sm:gap-1">
          {/* Category */}
          {listing.category && (
            <span className="text-[10px] sm:text-xs text-muted-foreground uppercase tracking-wide">
              {listing.category}
            </span>
          )}

          <h3 className="font-semibold text-foreground text-sm sm:text-base truncate leading-tight">
            {listing.name ?? "—"}
          </h3>

          {/* Price section */}
          <div className="mt-auto pt-1 sm:pt-1.5 flex flex-col gap-0.5">
            {/* Row 1: Main price + original crossed out */}
            <div className="flex items-center justify-between gap-1">
              <div className="flex items-baseline gap-1.5 whitespace-nowrap">
                {hasDiscount ? (
                  <>
                    <span className="text-base sm:text-lg font-bold text-red-600">
                      {formatPrice(listing.discountedPrice)}
                    </span>
                    <span className="text-[10px] sm:text-xs text-muted-foreground line-through">
                      {formatPrice(listing.originalPrice)}
                    </span>
                  </>
                ) : (
                  <span className="text-base sm:text-lg font-bold text-foreground">
                    {formatPrice(listing.originalPrice)}
                  </span>
                )}
              </div>

              {isLowStock && (
                <span className="text-[10px] sm:text-xs font-medium text-orange-600 whitespace-nowrap">
                  {tc("lowStock", { count: stock })}
                </span>
              )}
            </div>

            {/* Row 2: Loyalty "your price" */}
            {hasLoyalty && (
              <div className="flex items-center gap-1.5">
                <Crown className="h-3 w-3 text-amber-500 shrink-0" />
                <span className="text-xs sm:text-sm font-bold text-amber-600">
                  {formatPrice(listing.loyaltyPrice)}
                </span>
                <span className="text-[10px] sm:text-xs text-amber-600/70">
                  {tc("yourPrice")}
                </span>
              </div>
            )}
          </div>
        </div>
      </motion.div>
    </Link>
  );
}
