"use client";

import { useState } from "react";
import { Sparkles } from "lucide-react";
import { useTranslations } from "next-intl";
import { cn } from "@radolfa/shared/lib/utils";
import { Skeleton } from "@radolfa/shared/ui/skeleton";
import { useMyOrdersInfinite, useOrderSummary, type OrderFilter } from "@/features/profile/api";
import { ProfileOrderCard, HomeDeliveryIcon, PickupIcon } from "@/features/profile/ui/ProfileOrderCard";

const FILTERS: { value: OrderFilter; labelKey: string; countKey: keyof NonNullable<ReturnType<typeof useOrderSummary>["data"]> }[] = [
  { value: "all", labelKey: "filterAll", countKey: "all" },
  { value: "progress", labelKey: "filterInProgress", countKey: "progress" },
  { value: "delivered", labelKey: "filterDelivered", countKey: "delivered" },
  { value: "returns", labelKey: "filterReturns", countKey: "returns" },
];

/** Dashed empty state — design-system "no data" block (no fabricated content). */
function EmptyBlock({ message }: { message: string }) {
  return (
    <div className="border border-dashed border-ink/15 rounded-xl p-12 flex flex-col items-center text-center gap-3">
      <Sparkles className="h-10 w-10 text-ink/20" />
      <p className="text-sm text-ink/55">{message}</p>
    </div>
  );
}

/**
 * Phase 8 — Orders route. Server-side filter pills (All / In progress /
 * Delivered / Returns) drive `/orders/my-orders?filter=…`; the header
 * summary comes from `/orders/my-orders/summary`. No client-side
 * `.filter()/.sort()/.slice()` on `content[]` (CLAUDE.md, non-negotiable).
 */
export default function ProfileOrdersPage() {
  const t = useTranslations("profile");
  const [filter, setFilter] = useState<OrderFilter>("all");

  const { data: summary } = useOrderSummary();
  const {
    data,
    isLoading,
    isFetchingNextPage,
    hasNextPage,
    fetchNextPage,
  } = useMyOrdersInfinite(filter);

  const orders = data?.pages.flatMap((page) => page.content) ?? [];
  const total = summary?.all ?? 0;
  const inProgress = summary?.progress ?? 0;

  return (
    <section className="space-y-4 lg:space-y-5">
      <div className="flex items-end justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-[24px] lg:text-[28px] font-black leading-tight">{t("sectionOrders")}</h1>
          <p className="text-ink/55 text-[12px] lg:text-[13px] mt-1">
            {t("ordersCountSummary", { total, progress: inProgress })}
          </p>
        </div>
        <div className="flex items-center gap-2 text-[12px] overflow-x-auto scrollbar-hide">
          {FILTERS.map((f) => {
            const isActive = filter === f.value;
            const count = summary?.[f.countKey];
            return (
              <button
                key={f.value}
                type="button"
                onClick={() => setFilter(f.value)}
                className={cn(
                  "px-3.5 py-2 rounded-full border-2 font-bold whitespace-nowrap transition-colors",
                  isActive
                    ? "bg-ink text-white border-ink lg:bg-ink lg:border-ink"
                    : "bg-white text-ink/60 border-ink/12"
                )}
              >
                {t(f.labelKey)}
                {count != null && ` (${count})`}
              </button>
            );
          })}
        </div>
      </div>

      {/* Legend */}
      <div className="flex flex-wrap items-center gap-2 lg:gap-2 text-[11px] lg:text-[12px] text-ink/55">
        <span className="inline-flex items-center gap-1.5">
          <HomeDeliveryIcon className="text-mag" size={14} />
          {t("legendConfirmationCode")}
        </span>
        <span className="opacity-40 hidden lg:inline">·</span>
        <span className="inline-flex items-center gap-1.5">
          <PickupIcon className="text-mag" size={14} />
          {t("legendPickupQr")}
        </span>
      </div>

      {/* Order list */}
      {isLoading ? (
        <div className="space-y-4">
          {[...Array(3)].map((_, i) => (
            <Skeleton key={i} className="h-40 w-full rounded-2xl lg:rounded-3xl" />
          ))}
        </div>
      ) : orders.length === 0 ? (
        <EmptyBlock message={t("noOrders")} />
      ) : (
        <div className="space-y-3 lg:space-y-4">
          {orders.map((order) => (
            <ProfileOrderCard key={order.id} order={order} />
          ))}
        </div>
      )}

      {hasNextPage && (
        <div className="flex justify-center pt-2">
          <button
            type="button"
            onClick={() => fetchNextPage()}
            disabled={isFetchingNextPage}
            className="h-10 px-6 rounded-full border-2 border-ink/15 text-[13px] font-bold disabled:opacity-50"
          >
            {t("showMore")}
          </button>
        </div>
      )}
    </section>
  );
}
