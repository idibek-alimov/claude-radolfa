"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { BarChart2 } from "lucide-react";
import Link from "next/link";
import { fetchTopCampaigns } from "@/features/discount-management/api";

type ByOption = "revenue" | "units";
type PeriodOption = "7d" | "30d" | "90d";

function MedalIcon({ rank }: { rank: number }) {
  const colors = ["text-yellow-500", "text-slate-400", "text-amber-700"];
  const medals = ["🥇", "🥈", "🥉"];
  if (rank <= 3) {
    return <span className={`text-base ${colors[rank - 1]}`}>{medals[rank - 1]}</span>;
  }
  return <span className="text-sm font-bold text-muted-foreground tabular-nums">{rank}</span>;
}

export function TopCampaignsWidget() {
  const t = useTranslations("discount.topCampaigns");
  const [by, setBy] = useState<ByOption>("revenue");
  const [period, setPeriod] = useState<PeriodOption>("30d");

  const { data, isLoading } = useQuery({
    queryKey: ["top-campaigns", by, period],
    queryFn: () => fetchTopCampaigns({ by, period }),
    staleTime: 120_000,
  });

  const rows = data ?? [];

  return (
    <div>
      <div className="flex items-center justify-between mb-3">
        <h2 className="text-[11px] font-semibold uppercase tracking-widest text-zinc-400">
          {t("title")}
        </h2>

        {/* Segmented controls */}
        <div className="flex items-center gap-2">
          {/* By toggle */}
          <div className="flex rounded-lg border overflow-hidden text-xs">
            {(["revenue", "units"] as ByOption[]).map((opt) => (
              <button
                key={opt}
                onClick={() => setBy(opt)}
                className={`px-2.5 py-1 transition-colors ${
                  by === opt
                    ? "bg-primary text-white font-medium"
                    : "bg-white text-zinc-500 hover:bg-zinc-50"
                }`}
              >
                {opt === "revenue" ? t("byRevenue") : t("byUnits")}
              </button>
            ))}
          </div>

          {/* Period toggle */}
          <div className="flex rounded-lg border overflow-hidden text-xs">
            {(["7d", "30d", "90d"] as PeriodOption[]).map((opt) => {
              const label = opt === "7d" ? t("period7d") : opt === "30d" ? t("period30d") : t("period90d");
              return (
                <button
                  key={opt}
                  onClick={() => setPeriod(opt)}
                  className={`px-2.5 py-1 transition-colors ${
                    period === opt
                      ? "bg-primary text-white font-medium"
                      : "bg-white text-zinc-500 hover:bg-zinc-50"
                  }`}
                >
                  {label}
                </button>
              );
            })}
          </div>
        </div>
      </div>

      <div className="bg-white rounded-xl border border-zinc-100 shadow-sm overflow-hidden">
        {/* Column headers */}
        <div className="flex items-center gap-3 px-4 py-2.5 border-b border-zinc-100 bg-zinc-50/60">
          <div className="w-6 shrink-0" />
          <div className="flex-1 text-[10px] font-bold uppercase tracking-wider text-zinc-400">
            Campaign
          </div>
          <div className="w-20 text-[10px] font-bold uppercase tracking-wider text-zinc-400 text-right">
            {by === "revenue" ? t("uplift") : t("units")}
          </div>
          <div className="w-20 text-[10px] font-bold uppercase tracking-wider text-zinc-400 text-right">
            {by === "revenue" ? t("units") : t("uplift")}
          </div>
        </div>

        {isLoading ? (
          <div className="p-4 space-y-2">
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} className="h-11 rounded-lg bg-zinc-100 animate-pulse" />
            ))}
          </div>
        ) : rows.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-12 gap-2 text-zinc-400">
            <BarChart2 className="h-8 w-8 text-zinc-200" strokeWidth={1.5} />
            <p className="text-sm">{t("empty")}</p>
          </div>
        ) : (
          <div className="divide-y divide-zinc-50">
            {rows.map((row, idx) => (
              <Link
                key={row.campaign.id}
                href={`/manage/discounts/${row.campaign.id}`}
                className="flex items-center gap-3 px-4 py-3 hover:bg-zinc-50 transition-colors"
              >
                {/* Rank */}
                <div className="w-6 shrink-0 flex items-center justify-center">
                  <MedalIcon rank={idx + 1} />
                </div>

                {/* Color chip + title */}
                <div className="flex items-center gap-2 flex-1 min-w-0">
                  <div
                    className="h-2.5 w-2.5 shrink-0 rounded-full"
                    style={{ backgroundColor: `#${row.campaign.colorHex}` }}
                  />
                  <p className="text-sm font-semibold text-zinc-800 truncate">{row.campaign.title}</p>
                  <span className="text-xs text-zinc-400 shrink-0">−{row.campaign.amountValue}{row.campaign.amountType === "FIXED" ? " TJS" : "%"}</span>
                </div>

                {/* Primary stat */}
                <div className="w-20 text-right text-sm font-semibold tabular-nums text-zinc-800">
                  {by === "revenue"
                    ? row.revenueUplift.toLocaleString(undefined, { maximumFractionDigits: 0 })
                    : row.unitsMoved.toLocaleString()}
                </div>

                {/* Secondary stat */}
                <div className="w-20 text-right text-xs tabular-nums text-zinc-400">
                  {by === "revenue"
                    ? row.unitsMoved.toLocaleString()
                    : row.revenueUplift.toLocaleString(undefined, { maximumFractionDigits: 0 })}
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
