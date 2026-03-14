"use client";

import { useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { motion } from "framer-motion";
import { Flame } from "lucide-react";
import type { ListingVariant } from "@/entities/product";
import { Badge } from "@/shared/ui/badge";
import { formatPrice } from "@/shared/lib/format";
import { useTranslations } from "next-intl";

interface ProductCardProps {
  listing: ListingVariant;
}

const LOW_STOCK_THRESHOLD = 5;

export default function ProductCard({ listing }: ProductCardProps) {
  const t = useTranslations("productDetail");
  const tc = useTranslations("common");
  const [isHovered, setIsHovered] = useState(false);

  const coverImage = listing.images[0] ?? null;
  const hoverImage = listing.images[1] ?? null;
  const hasTier = listing.tierPriceStart != null;
  const displayPrice = hasTier ? listing.tierPriceStart! : listing.priceStart;
  const displayEnd = hasTier ? listing.tierPriceEnd! : listing.priceEnd;
  const hasRange = displayPrice !== displayEnd;
  const stock = listing.totalStock ?? 0;
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

          {/* Price + low stock */}
          <div className="mt-auto flex items-center justify-between gap-1 pt-1 sm:pt-1.5">
            <div className="flex items-baseline gap-1.5 whitespace-nowrap">
              {hasTier && (
                <span className="text-[10px] sm:text-xs text-muted-foreground line-through">
                  {formatPrice(listing.priceStart)}
                </span>
              )}
              <span className="text-base sm:text-lg font-bold text-primary">
                {hasRange && (
                  <span className="text-[10px] sm:text-xs font-medium text-muted-foreground mr-1">
                    {t("priceFrom")}
                  </span>
                )}
                {formatPrice(displayPrice)}
              </span>
            </div>

            {isLowStock && (
              <span className="text-[10px] sm:text-xs font-medium text-orange-600 whitespace-nowrap">
                {tc("lowStock", { count: stock })}
              </span>
            )}
          </div>
        </div>
      </motion.div>
    </Link>
  );
}
