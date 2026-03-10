"use client";

import Image from "next/image";
import Link from "next/link";
import { motion } from "framer-motion";
import type { ListingVariant } from "@/entities/product";
import { Badge } from "@/shared/ui/badge";
import StockBadge from "./StockBadge";
import { formatPrice } from "@/shared/lib/format";
import { useTranslations } from "next-intl";

interface ProductCardProps {
  listing: ListingVariant;
}

/**
 * Self-contained card that renders a single listing variant (colour).
 * Links to the product detail page via its slug.
 */
export default function ProductCard({ listing }: ProductCardProps) {
  const t = useTranslations("productDetail");
  const coverImage = listing.images[0] ?? null;
  const hasRange = listing.priceStart !== listing.priceEnd;

  return (
    <Link href={`/products/${listing.slug}`} className="group block">
      <motion.div
        whileHover={{ y: -4 }}
        transition={{ duration: 0.2 }}
        className="relative rounded-lg sm:rounded-xl border bg-card text-card-foreground shadow-sm hover:shadow-lg transition-shadow overflow-hidden flex flex-col h-full"
      >
        {/* Colour badge */}
        {listing.colorKey && (
          <Badge
            variant="secondary"
            className="absolute top-2 right-2 z-10 text-[10px] sm:text-xs sm:top-3 sm:right-3"
          >
            {listing.colorKey}
          </Badge>
        )}

        {/* Cover image */}
        <div className="relative w-full aspect-[4/5] sm:aspect-[4/4] bg-muted overflow-hidden">
          {coverImage ? (
            <Image
              src={coverImage}
              alt={listing.name ?? "Product image"}
              fill
              className="object-cover group-hover:scale-105 transition-transform duration-300"
              unoptimized
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center">
              <span className="text-muted-foreground text-xs sm:text-sm">No image</span>
            </div>
          )}
        </div>

        {/* Body */}
        <div className="p-2.5 sm:p-4 flex flex-col flex-1 gap-1 sm:gap-2">
          <h3 className="font-semibold text-foreground text-sm sm:text-base truncate">
            {listing.name ?? "—"}
          </h3>

          <p className="hidden sm:block text-sm text-muted-foreground line-clamp-2 min-h-[2.5rem]">
            {listing.webDescription ?? "No description yet."}
          </p>

          {/* Price + stock */}
          <div className="mt-auto flex flex-col sm:flex-row sm:items-center sm:justify-between gap-1.5 sm:gap-0 pt-1 sm:pt-2">
            <span className="text-base sm:text-lg font-bold text-primary whitespace-nowrap">
              {hasRange && (
                <span className="text-[10px] sm:text-xs font-medium text-muted-foreground mr-1">
                  {t("priceFrom")}
                </span>
              )}
              {formatPrice(listing.priceStart)}
            </span>
            <StockBadge stock={listing.totalStock} />
          </div>
        </div>
      </motion.div>
    </Link>
  );
}
