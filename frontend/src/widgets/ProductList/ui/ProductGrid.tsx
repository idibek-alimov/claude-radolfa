"use client";

import { ProductCard, ProductCardSkeleton } from "@/entities/product";
import type { ProductListProps } from "@/widgets/ProductList";

const SKELETON_COUNT = 8;

export default function ProductGrid({
  products,
  loading = false,
  hasMore = false,
  onLoadMore,
}: ProductListProps) {
  return (
    <div className="flex flex-col items-center gap-8">
      {/* Card grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5 w-full">
        {products.map((product) => (
          <ProductCard key={product.erpId} product={product} />
        ))}

        {/* Skeleton placeholders during loading */}
        {loading &&
          products.length === 0 &&
          Array.from({ length: SKELETON_COUNT }).map((_, i) => (
            <ProductCardSkeleton key={`skeleton-${i}`} />
          ))}
      </div>

      {/* Loading more indicator */}
      {loading && products.length > 0 && (
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

      {!loading && !hasMore && products.length > 0 && (
        <p className="text-muted-foreground text-sm">All products loaded.</p>
      )}

      {!loading && products.length === 0 && (
        <p className="text-muted-foreground text-sm py-8">No products found.</p>
      )}
    </div>
  );
}
