"use client";

import { useState } from "react";
import { X } from "lucide-react";
import { toast } from "sonner";
import { Skeleton } from "@/shared/ui/skeleton";
import { Button } from "@/shared/ui/button";
import { usePickpointSummaries } from "@/entities/pickpoint";
import { getErrorMessage } from "@/shared/lib";
import { PickpointSummaryCard } from "./PickpointSummaryCard";

export function PickpointOverviewPage() {
  const { data, isLoading, isError, error } = usePickpointSummaries();
  const [bannerDismissed, setBannerDismissed] = useState(false);

  if (isError) {
    toast.error(getErrorMessage(error, "Failed to load pickpoint summaries"));
  }

  const overdueTotal       = data?.reduce((s, p) => s + p.overdue, 0) ?? 0;
  const pickpointsWithOverdue = data?.filter((p) => p.overdue > 0).length ?? 0;
  const showBanner = overdueTotal > 0 && !bannerDismissed;

  return (
    <div className="flex flex-col flex-1 gap-6">
      <div>
        <h1 className="text-2xl font-semibold">Pickup Points</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Live operational counts across all active pickup locations.
        </p>
      </div>

      {showBanner && (
        <div className="flex items-start justify-between gap-3 rounded-xl border border-rose-200 bg-rose-50 p-4 text-rose-700">
          <p className="text-sm font-medium">
            {overdueTotal} package{overdueTotal !== 1 ? "s" : ""} across{" "}
            {pickpointsWithOverdue} pickup point{pickpointsWithOverdue !== 1 ? "s" : ""} are
            overdue. Review and initiate returns.
          </p>
          <Button
            variant="ghost"
            size="sm"
            className="h-6 w-6 p-0 text-rose-600 hover:text-rose-700 hover:bg-rose-100 shrink-0"
            onClick={() => setBannerDismissed(true)}
          >
            <X className="h-4 w-4" />
          </Button>
        </div>
      )}

      {isLoading && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {[...Array(3)].map((_, i) => (
            <Skeleton key={i} className="h-36 rounded-xl" />
          ))}
        </div>
      )}

      {!isLoading && data?.length === 0 && (
        <div className="flex flex-col items-center justify-center rounded-xl border border-dashed p-12 text-muted-foreground">
          <p className="text-sm">No active pickup points.</p>
        </div>
      )}

      {!isLoading && data && data.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {data.map((summary) => (
            <PickpointSummaryCard key={summary.pickpointId} summary={summary} />
          ))}
        </div>
      )}
    </div>
  );
}
