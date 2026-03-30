"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { ChevronLeft } from "lucide-react";
import { RatingSummaryCard, ReviewList, ReviewVariantFilterStrip } from "@/entities/review";
import type { VariantPill } from "@/entities/review";
import { fetchListingBySlug } from "@/entities/product/api";

function formatColorKey(key: string): string {
  return key.replace(/_/g, " ").replace(/\b\w/g, (c) => c.toUpperCase());
}

interface ProductReviewsPageProps {
  slug: string;
}

export function ProductReviewsPage({ slug }: ProductReviewsPageProps) {
  const t = useTranslations("reviews.page");

  const { data: listing } = useQuery({
    queryKey: ["listing", slug],
    queryFn: () => fetchListingBySlug(slug),
  });

  const variants: VariantPill[] = listing
    ? [
        {
          slug: listing.slug,
          label: listing.colorDisplayName,
          thumbnail: listing.images[0] ?? null,
          isActive: true,
        },
        ...listing.siblingVariants.map((s) => ({
          slug: s.slug,
          label: formatColorKey(s.colorKey),
          thumbnail: s.thumbnail,
          isActive: false,
        })),
      ]
    : [];

  return (
    <div className="container mx-auto px-4 py-8 max-w-6xl">
      {/* Back link */}
      <Link
        href={`/products/${slug}`}
        className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground mb-6"
      >
        <ChevronLeft className="h-4 w-4" />
        {t("back")}
      </Link>

      {/* Two-column layout: main + sticky sidebar */}
      <div className="lg:grid lg:grid-cols-[1fr_320px] gap-8 items-start">
        {/* Main column */}
        <div className="space-y-4">
          <ReviewVariantFilterStrip variants={variants} />
          <ReviewList slug={slug} mode="full" />
        </div>

        {/* Sticky sidebar — stacks below reviews on mobile */}
        <aside className="mt-6 lg:mt-0 lg:sticky lg:top-6">
          <RatingSummaryCard slug={slug} />
        </aside>
      </div>
    </div>
  );
}
