"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { ChevronLeft } from "lucide-react";
import { RatingSummaryCard, ReviewList } from "@/entities/review";

interface ProductReviewsPageProps {
  slug: string;
}

export function ProductReviewsPage({ slug }: ProductReviewsPageProps) {
  const t = useTranslations("reviews.page");

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
      <div className="lg:grid lg:grid-cols-[1fr_300px] gap-8 items-start">
        {/* Main column */}
        <div>
          <ReviewList slug={slug} mode="full" />
        </div>

        {/* Sticky sidebar */}
        <aside className="lg:sticky lg:top-24 space-y-6">
          <RatingSummaryCard slug={slug} />

          <div className="border-t pt-6 space-y-3">
            <p className="text-sm font-medium">{t("sidebarAsk")}</p>
            <Link
              href={`/products/${slug}/questions`}
              className="inline-flex w-full items-center justify-center rounded-lg border border-input bg-background px-4 py-2 text-sm font-medium hover:bg-accent hover:text-accent-foreground transition-colors"
            >
              {t("askButton")}
            </Link>
          </div>
        </aside>
      </div>
    </div>
  );
}
