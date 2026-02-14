"use client";

import Image from "next/image";
import Link from "next/link";
import { motion } from "framer-motion";
import type { ListingVariant } from "@/entities/product";
import { Badge } from "@/shared/ui/badge";
import StockBadge from "./StockBadge";
import { formatPriceRange } from "@/shared/lib/format";

interface ProductCardProps {
  listing: ListingVariant;
}

/**
 * Self-contained card that renders a single listing variant (colour).
 * Links to the product detail page via its slug.
 */
export default function ProductCard({ listing }: ProductCardProps) {
  const coverImage = listing.images[0] ?? null;

  return (
    <Link href={`/products/${listing.slug}`} className="group block">
      <motion.div
        whileHover={{ y: -4 }}
        transition={{ duration: 0.2 }}
        className="relative rounded-xl border bg-card text-card-foreground shadow-sm hover:shadow-lg transition-shadow overflow-hidden flex flex-col h-full"
      >
        {/* Colour badge */}
        {listing.colorKey && (
          <Badge
            variant="secondary"
            className="absolute top-3 right-3 z-10"
          >
            {listing.colorKey}
          </Badge>
        )}

        {/* Cover image */}
        <div className="relative w-full h-52 bg-muted overflow-hidden">
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
              <span className="text-muted-foreground text-sm">No image</span>
            </div>
          )}
        </div>

        {/* Body */}
        <div className="p-4 flex flex-col flex-1 gap-2">
          <h3 className="font-semibold text-foreground truncate">
            {listing.name ?? "—"}
          </h3>

          <p className="text-sm text-muted-foreground line-clamp-2 min-h-[2.5rem]">
            {listing.webDescription ?? "No description yet."}
          </p>

          {/* Price + stock row */}
          <div className="mt-auto flex items-center justify-between pt-2">
            <span className="text-lg font-bold text-primary">
              {formatPriceRange(listing.priceStart, listing.priceEnd)}
            </span>
            <StockBadge stock={listing.totalStock} />
          </div>
        </div>
      </motion.div>
    </Link>
  );
}
