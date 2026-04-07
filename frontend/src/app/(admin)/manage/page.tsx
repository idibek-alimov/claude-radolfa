"use client";

import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { useAuth } from "@/features/auth";
import { reindexSearch } from "@/features/search/api";
import type { ReindexResult } from "@/features/search/api";
import { Button } from "@/shared/ui/button";
import { getErrorMessage } from "@/shared/lib";
import { RefreshCw, Loader2 } from "lucide-react";
import { useTranslations } from "next-intl";
import { DashboardKpiRow, ModerationAlertsRow, ActiveDiscountsWidget, OrderSummaryWidget } from "@/widgets/AdminDashboard";

export default function ManageDashboardPage() {
  const t = useTranslations("manage");
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  const [reindexResult, setReindexResult] = useState<ReindexResult | null>(null);

  const reindexMutation = useMutation({
    mutationFn: reindexSearch,
    onSuccess: (result) => setReindexResult(result),
    onError: (err: unknown) => toast.error(getErrorMessage(err, "Reindex failed")),
  });

  return (
    <div className="space-y-8">
      {/* Page header */}
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-zinc-900">
          {t("dashboardTitle")}
        </h1>
        <p className="mt-1 text-sm text-zinc-500">
          {t("dashboardSubtitle")}
        </p>
      </div>

      {/* Row 1 — KPI cards */}
      <DashboardKpiRow />

      {/* Row 2 — Moderation alerts */}
      <ModerationAlertsRow />

      {/* Row 3 — Active discount campaigns */}
      <ActiveDiscountsWidget />

      {/* Row 4 — Orders & Revenue (ADMIN only) */}
      {isAdmin && <OrderSummaryWidget />}

      {/* Search Tools — ADMIN only */}
      {isAdmin && (
        <div>
          <h2 className="text-[11px] font-semibold uppercase tracking-widest text-zinc-400 mb-3">
            {t("searchToolsTitle")}
          </h2>
          <div className="bg-white rounded-xl border border-zinc-100 shadow-sm p-4">
            <div className="flex items-center gap-3 flex-wrap">
              <Button
                variant="outline"
                onClick={() => {
                  setReindexResult(null);
                  reindexMutation.mutate();
                }}
                disabled={reindexMutation.isPending}
                className="gap-1.5"
              >
                {reindexMutation.isPending ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    {t("reindexing")}
                  </>
                ) : (
                  <>
                    <RefreshCw className="h-4 w-4" />
                    {t("reindexButton")}
                  </>
                )}
              </Button>
              {reindexResult && (
                <p className="text-sm text-zinc-400">
                  {t("reindexResult", {
                    count: reindexResult.indexed,
                    errors: reindexResult.errorCount,
                  })}
                </p>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
