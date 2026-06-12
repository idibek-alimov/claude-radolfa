"use client";

import { useMemo } from "react";
import { ProductCard, ProductCardSkeleton } from "@/entities/product";
import type { ListingVariant } from "@/entities/product";
import type { ProductListProps } from "@/widgets/ProductList";

const SKELETON_COUNT = 10;
const CHUNK_SIZE = 10;

function chunk<T>(arr: T[], size: number): T[][] {
  const chunks: T[][] = [];
  for (let i = 0; i < arr.length; i += size) {
    chunks.push(arr.slice(i, i + size));
  }
  return chunks;
}

function getDominantCategory(items: ListingVariant[]): string | null {
  const counts: Record<string, number> = {};
  for (const item of items) {
    if (item.categoryName) {
      counts[item.categoryName] = (counts[item.categoryName] || 0) + 1;
    }
  }
  let max = 0;
  let dominant: string | null = null;
  for (const [cat, count] of Object.entries(counts)) {
    if (count > max) {
      max = count;
      dominant = cat;
    }
  }
  return dominant;
}

function SectionBreak({ label }: { label: string | null }) {
  return (
    <div className="w-full flex items-center gap-4 py-2">
      <div className="flex-1 h-px bg-border" />
      {label && (
        <span className="text-xs sm:text-sm font-medium text-muted-foreground uppercase tracking-wider shrink-0">
          {label}
        </span>
      )}
      <div className="flex-1 h-px bg-border" />
    </div>
  );
}

const GRID_CLASSES =
  "grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 2xl:grid-cols-5 gap-2 sm:gap-3 w-full";

export default function ProductGrid({
  listings,
  loading = false,
  hasMore = false,
  onLoadMore,
}: ProductListProps) {
  const chunks = useMemo(() => chunk(listings, CHUNK_SIZE), [listings]);

  return (
    <div className="flex flex-col items-center gap-6">
      {chunks.map((group, chunkIndex) => (
        <div key={chunkIndex} className="w-full">
          {chunkIndex > 0 && (
            <div className="mb-4">
              <SectionBreak label={getDominantCategory(group)} />
            </div>
          )}
          <div className={GRID_CLASSES}>
            {group.map((listing) => (
              <ProductCard key={listing.slug} listing={listing} />
            ))}
          </div>
        </div>
      ))}

      {/* Skeleton placeholders during initial load */}
      {loading && listings.length === 0 && (
        <div className={GRID_CLASSES}>
          {Array.from({ length: SKELETON_COUNT }).map((_, i) => (
            <ProductCardSkeleton key={`skeleton-${i}`} />
          ))}
        </div>
      )}

      {/* Loading more indicator */}
      {loading && listings.length > 0 && (
        <div className="flex gap-3">
          {Array.from({ length: 4 }).map((_, i) => (
            <ProductCardSkeleton key={`skeleton-more-${i}`} />
          ))}
        </div>
      )}

      {!loading && hasMore && onLoadMore && (
        <button
          onClick={onLoadMore}
          className="px-8 py-2.5 bg-primary text-primary-foreground rounded-lg hover:bg-primary/90 transition-colors text-sm font-medium"
        >
          Load More
        </button>
      )}

      {!loading && !hasMore && listings.length > 0 && (
        <p className="text-muted-foreground text-sm">All products loaded.</p>
      )}

      {!loading && listings.length === 0 && (
        <p className="text-muted-foreground text-sm py-8">No products found.</p>
      )}
    </div>
  );
}
