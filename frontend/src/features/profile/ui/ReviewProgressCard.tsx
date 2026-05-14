"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Star } from "lucide-react";
import { Skeleton } from "@/shared/ui/skeleton";
import { getMyOrders } from "@/features/profile/api";
import { fetchReviewReward } from "@/entities/loyalty/api";
import type { Order } from "@/features/profile/types";

export function ReviewProgressCard() {
  const t = useTranslations("profile");

  const { data: orders, isLoading: ordersLoading } = useQuery<Order[]>({
    queryKey: ["my-orders"],
    queryFn: getMyOrders,
  });

  const { data: rewardData, isLoading: rewardLoading } = useQuery({
    queryKey: ["loyalty-review-reward"],
    queryFn: fetchReviewReward,
    staleTime: Infinity,
  });

  if (ordersLoading || rewardLoading) {
    return <Skeleton className="h-24 w-full rounded-xl" />;
  }

  if (!orders) return null;

  const delivered = orders.filter((o) => o.status === "DELIVERED");
  const total = delivered.flatMap((o) => o.items).length;

  if (total === 0) return null;

  const reviewed = delivered
    .flatMap((o) => o.items)
    .filter((i) => i.hasReviewed).length;

  const percent = total === 0 ? 0 : Math.round((reviewed / total) * 100);
  const allDone = reviewed === total;
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
          : t("reviewProgressSubtitle", { reviewed, total })}
      </p>

      {/* Progress bar */}
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
