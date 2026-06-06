"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { fetchCategoryTree } from "@/entities/product/api";
import { useLoyaltyTiers } from "@/entities/loyalty";
import { Skeleton } from "@radolfa/shared/ui/skeleton";

/**
 * Flat categories ribbon rendered inside the desktop header.
 * B-Magenta reference: <!-- categories ribbon -->
 */
export function MegaMenu() {
  const { data: categories, isLoading: catsLoading } = useQuery({
    queryKey: ["categories", "tree"],
    queryFn: fetchCategoryTree,
    staleTime: 30 * 60 * 1000,
  });

  const { data: tiers, isLoading: tiersLoading } = useLoyaltyTiers();
  const topTier = tiers?.find((t) => t.displayOrder === 1);

  if (catsLoading) {
    return (
      <nav className="border-t border-ink/5 hidden md:block">
        <div className="max-w-[1440px] mx-auto px-6 h-11 flex items-center gap-6">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-4 w-16" />
          ))}
        </div>
      </nav>
    );
  }

  return (
    <nav className="border-t border-ink/5 hidden md:block">
      <div className="max-w-[1440px] mx-auto px-6 h-11 flex items-center gap-6 text-[13px] font-medium text-ink/80 overflow-x-auto scrollbar-hide">
        {/* All categories */}
        <Link
          href="/products"
          className="inline-flex items-center gap-1.5 whitespace-nowrap font-semibold text-mag shrink-0"
        >
          <svg
            width="13"
            height="13"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2.3"
            aria-hidden
          >
            <line x1="4" y1="6" x2="20" y2="6" />
            <line x1="4" y1="12" x2="20" y2="12" />
            <line x1="4" y1="18" x2="20" y2="18" />
          </svg>
          All categories
        </Link>

        {/* Mega sale */}
        <Link
          href="/collections/on_sale"
          className="whitespace-nowrap text-sale font-bold shrink-0"
        >
          🔥 Mega sale
        </Link>

        {/* Dynamic root categories */}
        {categories?.map((cat) => (
          <Link
            key={cat.id}
            href={`/categories/${cat.slug}/products`}
            className="whitespace-nowrap hover:text-mag transition-colors shrink-0"
          >
            {cat.name}
          </Link>
        ))}

        {/* Crown tier — pinned right */}
        <Link
          href="/loyalty"
          className="ml-auto inline-flex items-center gap-1.5 whitespace-nowrap font-bold shrink-0"
          style={{ color: "#9A6E0F" }}
        >
          <svg
            width="13"
            height="13"
            viewBox="0 0 24 24"
            fill="currentColor"
            className="text-[#FFCC4F]"
            aria-hidden
          >
            <path d="M5 16l-3-8 5.5 4L12 4l4.5 8L22 8l-3 8H5z" />
          </svg>
          {tiersLoading || !topTier
            ? "Crown tier"
            : `Crown tier · ${topTier.discountPercentage}% off`}
        </Link>
      </div>
    </nav>
  );
}
