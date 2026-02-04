"use client";

import { ProductCard } from "@/entities/product";
import type { ProductListProps } from "@/widgets/ProductList";

/**
 * Responsive product grid with a "Load More" pagination trigger.
 *
 * Layout breakpoints (Tailwind):
 *   sm  – 2 columns
 *   lg  – 3 columns
 *   xl  – 4 columns
 */
export default function ProductGrid({
  products,
  loading = false,
  hasMore = false,
  onLoadMore,
}: ProductListProps) {
  return (
    <div className="flex flex-col items-center gap-6">
      {/* ── Card grid ───────────────────────────────────────────── */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5 w-full">
        {products.map((product) => (
          <ProductCard key={product.erpId} product={product} />
        ))}
      </div>

      {/* ── Feedback states ─────────────────────────────────────── */}
      {loading && (
        <p className="text-gray-500 text-sm animate-pulse">Loading…</p>
      )}

      {!loading && hasMore && onLoadMore && (
        <button
          onClick={onLoadMore}
          className="px-6 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 transition-colors text-sm font-medium"
        >
          Load More
        </button>
      )}

      {!loading && !hasMore && products.length > 0 && (
        <p className="text-gray-400 text-sm">All products loaded.</p>
      )}

      {!loading && products.length === 0 && (
        <p className="text-gray-400 text-sm">No products found.</p>
      )}
    </div>
  );
}
