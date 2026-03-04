"use client";

import Image from "next/image";
import Link from "next/link";
import { motion } from "framer-motion";
import { ImageOff, Heart } from "lucide-react";
import type { ListingVariant } from "@/entities/product";
import StockBadge from "./StockBadge";
import { formatPriceRange } from "@/shared/lib/format";
import { useWishlist } from "@/shared/lib/wishlist";
import { cn } from "@/shared/lib/utils";

interface ProductCardProps {
  listing: ListingVariant;
}

export default function ProductCard({ listing }: ProductCardProps) {
  const coverImage = listing.images[0] ?? null;
  const { wishlisted, toggle } = useWishlist(listing.slug);

  return (
    <Link
      href={`/products/${listing.slug}`}
      aria-label={listing.name ?? "View product"}
      className="group block"
    >
      <motion.div
        whileHover={{ y: -4 }}
        transition={{ duration: 0.2 }}
        className="relative rounded-xl border bg-card text-card-foreground shadow-sm hover:shadow-lg transition-shadow overflow-hidden flex flex-col h-full"
      >
        {/* Image area */}
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
            <div className="w-full h-full flex flex-col items-center justify-center gap-2 text-muted-foreground/40">
              <ImageOff className="h-10 w-10" strokeWidth={1.5} />
            </div>
          )}

          {/* Wishlist button — top left */}
          <button
            onClick={toggle}
            aria-label={wishlisted ? "Remove from wishlist" : "Add to wishlist"}
            className="absolute top-2 left-2 z-10 p-1.5 rounded-full bg-white/80 backdrop-blur-sm hover:bg-white transition-colors shadow-sm"
          >
            <Heart
              className={cn(
                "h-4 w-4 transition-colors",
                wishlisted ? "fill-red-500 text-red-500" : "text-muted-foreground"
              )}
            />
          </button>

          {/* Promo badges — top right stack */}
          <div className="absolute top-2 right-2 z-10 flex flex-col gap-1 items-end">
            {listing.topSelling && (
              <span className="text-[10px] font-bold uppercase tracking-wide bg-amber-500 text-white px-2 py-0.5 rounded-full shadow-sm">
                Bestseller
              </span>
            )}
            {listing.featured && (
              <span className="text-[10px] font-bold uppercase tracking-wide bg-primary text-primary-foreground px-2 py-0.5 rounded-full shadow-sm">
                Featured
              </span>
            )}
          </div>

          {/* Color swatch — bottom left */}
          {listing.colorHexCode && (
            <div className="absolute bottom-2 left-2 z-10 flex items-center gap-1.5 bg-black/40 backdrop-blur-sm rounded-full px-2 py-0.5">
              <span
                className="w-3 h-3 rounded-full ring-1 ring-white/60 flex-shrink-0"
                style={{ backgroundColor: listing.colorHexCode }}
              />
              {listing.colorKey && (
                <span className="text-[10px] text-white font-medium leading-none">
                  {listing.colorKey}
                </span>
              )}
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
