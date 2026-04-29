"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { fetchRatingSummary } from "../api";
import { StarRating } from "@/shared/ui/StarRating";

interface RatingSummaryCardProps {
  slug: string;
  onWriteReview?: () => void;
  activeRating?: number | null;
  onRatingFilterChange?: (rating: number | null) => void;
}

export function RatingSummaryCard({ slug, onWriteReview, activeRating, onRatingFilterChange }: RatingSummaryCardProps) {
  const t = useTranslations("reviews.summary");

  const { data, isLoading } = useQuery({
    queryKey: ["rating", slug],
    queryFn: () => fetchRatingSummary(slug),
  });

  if (isLoading) return <RatingSummarySkeleton />;
  if (!data || data.reviewCount === 0) return null;

  const totalSizeFeedback = data.sizeAccurate + data.sizeRunsSmall + data.sizeRunsLarge;

  return (
    <div className="border rounded-xl p-5 space-y-5 bg-card">
      {/* Hero: average + stars + count */}
      <div className="flex items-center gap-4">
        <span className="text-4xl sm:text-5xl font-bold tracking-tight leading-none">
          {data.averageRating.toFixed(1)}
        </span>
        <div className="space-y-1">
          <StarRating rating={data.averageRating} size="md" />
          <p className="text-sm text-muted-foreground">
            {t("reviews", { count: data.reviewCount })}
          </p>
        </div>
      </div>

      {/* Distribution bars: 5 → 1 */}
      <div className="space-y-1.5">
        {[5, 4, 3, 2, 1].map((star) => {
          const count = data.distribution[star] ?? 0;
          const pct = data.reviewCount > 0 ? (count / data.reviewCount) * 100 : 0;
          const isActive = activeRating === star;
          const isFiltering = activeRating != null;
          const isClickable = !!onRatingFilterChange;
          return (
            <button
              key={star}
              type="button"
              disabled={!isClickable}
              onClick={isClickable ? () => onRatingFilterChange(isActive ? null : star) : undefined}
              className={`w-full flex items-center gap-2 text-sm rounded transition-opacity ${
                isClickable ? "cursor-pointer hover:opacity-90" : "cursor-default"
              } ${isFiltering && !isActive ? "opacity-40" : ""}`}
            >
              <span className="w-3 shrink-0 text-right text-muted-foreground">{star}</span>
              <span className="text-amber-400 shrink-0">★</span>
              <div className="flex-1 h-2 rounded-full bg-muted overflow-hidden">
                <div
                  className={`h-full rounded-full transition-all ${isActive ? "bg-amber-500" : "bg-amber-400"}`}
                  style={{ width: `${pct}%` }}
                />
              </div>
              {isActive && (
                <span className="shrink-0 w-2 h-2 rounded-full bg-primary" />
              )}
              <span className="w-8 shrink-0 text-right text-xs text-muted-foreground">
                {pct.toFixed(0)}%
              </span>
            </button>
          );
        })}
      </div>

      {/* Size fit */}
      {totalSizeFeedback > 0 && (
        <div className="space-y-2 border-t pt-4">
          <p className="text-sm font-medium">{t("sizeFit")}</p>
          <div className="flex flex-wrap gap-2 text-xs text-muted-foreground">
            <span className="bg-muted rounded-full px-3 py-1">
              {t("runsSmall")}: {data.sizeRunsSmall}
            </span>
            <span className="bg-muted rounded-full px-3 py-1">
              {t("trueToSize")}: {data.sizeAccurate}
            </span>
            <span className="bg-muted rounded-full px-3 py-1">
              {t("runsLarge")}: {data.sizeRunsLarge}
            </span>
          </div>
        </div>
      )}

      {/* CTA */}
      <button
        onClick={onWriteReview}
        className="w-full rounded-lg border border-primary text-primary text-sm font-medium py-2 hover:bg-primary hover:text-primary-foreground transition-colors"
      >
        {t("writeReview")}
      </button>
    </div>
  );
}

function RatingSummarySkeleton() {
  return (
    <div className="border rounded-xl p-5 space-y-5 animate-pulse">
      <div className="flex items-center gap-4">
        <div className="h-12 w-16 rounded bg-muted" />
        <div className="space-y-2">
          <div className="h-4 w-24 rounded bg-muted" />
          <div className="h-3 w-16 rounded bg-muted" />
        </div>
      </div>
      <div className="space-y-2">
        {[5, 4, 3, 2, 1].map((s) => (
          <div key={s} className="h-2 rounded bg-muted" />
        ))}
      </div>
    </div>
  );
}
