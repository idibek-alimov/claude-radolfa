"use client";

import { useQuery } from "@tanstack/react-query";
import { fetchRatingSummary } from "../api";
import { StarRating } from "@/shared/ui/StarRating";

interface RatingSummaryCardProps {
  slug: string;
}

export function RatingSummaryCard({ slug }: RatingSummaryCardProps) {
  const { data, isLoading } = useQuery({
    queryKey: ["rating", slug],
    queryFn: () => fetchRatingSummary(slug),
  });

  if (isLoading) return <RatingSummarySkeleton />;
  if (!data || data.reviewCount === 0) return null;

  const totalSizeFeedback = data.sizeAccurate + data.sizeRunsSmall + data.sizeRunsLarge;

  return (
    <div className="space-y-4">
      {/* Hero: average + count */}
      <div className="flex items-center gap-3">
        <span className="text-4xl font-bold">{data.averageRating.toFixed(1)}</span>
        <div className="space-y-0.5">
          <StarRating rating={data.averageRating} size="md" />
          <p className="text-sm text-muted-foreground">{data.reviewCount} reviews</p>
        </div>
      </div>

      {/* Distribution bars: 5 → 1 */}
      <div className="space-y-1">
        {[5, 4, 3, 2, 1].map((star) => {
          const count = data.distribution[star] ?? 0;
          const pct = data.reviewCount > 0 ? (count / data.reviewCount) * 100 : 0;
          return (
            <div key={star} className="flex items-center gap-2 text-sm">
              <span className="w-3 text-right">{star}</span>
              <span className="text-amber-400">★</span>
              <div className="flex-1 h-2 rounded-full bg-muted overflow-hidden">
                <div
                  className="h-full rounded-full bg-amber-400"
                  style={{ width: `${pct}%` }}
                />
              </div>
              <span className="w-8 text-right text-muted-foreground">{count}</span>
            </div>
          );
        })}
      </div>

      {/* Size fit — only shown if any feedback exists */}
      {totalSizeFeedback > 0 && (
        <div className="space-y-1">
          <p className="text-sm font-medium">Size fit</p>
          <div className="flex gap-3 text-sm text-muted-foreground">
            <span>Runs small: {data.sizeRunsSmall}</span>
            <span>True to size: {data.sizeAccurate}</span>
            <span>Runs large: {data.sizeRunsLarge}</span>
          </div>
        </div>
      )}
    </div>
  );
}

function RatingSummarySkeleton() {
  return (
    <div className="space-y-2 animate-pulse">
      <div className="h-10 w-40 rounded bg-muted" />
      <div className="space-y-1">
        {[5, 4, 3, 2, 1].map((s) => (
          <div key={s} className="h-2 rounded bg-muted" />
        ))}
      </div>
    </div>
  );
}
