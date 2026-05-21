"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Star } from "lucide-react";
import { Skeleton } from "@/shared/ui/skeleton";
import { useMyReviewProgress } from "@/features/profile/api";
import { fetchReviewReward } from "@/entities/loyalty/api";

export function ReviewProgressCard() {
  const t = useTranslations("profile");

  const { data: progress, isLoading: progressLoading } = useMyReviewProgress();

  const { data: rewardData, isLoading: rewardLoading } = useQuery({
    queryKey: ["loyalty-review-reward"],
    queryFn: fetchReviewReward,
    staleTime: Infinity,
  });

  if (progressLoading || rewardLoading) {
    return <Skeleton className="h-24 w-full rounded-xl" />;
  }

  if (!progress) return null;

  const { totalOrders, reviewedOrders } = progress;

  if (totalOrders === 0) return null;

  const percent = Math.round((reviewedOrders / totalOrders) * 100);
  const allDone = reviewedOrders >= totalOrders;
  const rewardPoints = rewardData?.points ?? 50;

  return (
    <div className="bg-accent/30 rounded-xl p-5 space-y-3 mb-6">
      <div className="flex items-center gap-2">
        <Star className="h-4 w-4 text-primary fill-primary" />
        <p className="text-sm font-semibold text-foreground">
          {t("reviewProgressTitle")}
        </p>
      </div>

      <p className="text-sm text-muted-foreground">
        {allDone
          ? t("reviewProgressAllDone")
          : t("reviewProgressSubtitle", { reviewed: reviewedOrders, total: totalOrders })}
      </p>

      <div className="h-2.5 bg-muted rounded-full overflow-hidden">
        <div
          className="h-full bg-primary rounded-full transition-all duration-500"
          style={{ width: `${percent}%` }}
        />
      </div>

      {!allDone && (
        <p className="text-xs text-muted-foreground">
          {t("earnPointsForNextReview", { points: rewardPoints })}
        </p>
      )}
    </div>
  );
}
