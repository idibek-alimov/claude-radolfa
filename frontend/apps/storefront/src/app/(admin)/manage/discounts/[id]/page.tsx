"use client";

import { useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { AlertTriangle, ArrowLeft, Edit2 } from "lucide-react";
import Link from "next/link";
import ProtectedRoute from "@/shared/components/ProtectedRoute";
import { Button } from "@/shared/ui/button";
import { Skeleton } from "@/shared/ui/skeleton";
import {
  fetchDiscountById,
  fetchDiscountMetrics,
  fetchAnalyticsConfig,
} from "@/features/discount-management/api";
import { MetricSparkline } from "@/features/discount-management/ui/MetricSparkline";
import type { DailyMetric } from "@/features/discount-management/model/types";

function KpiCard({ label, value, sub }: { label: string; value: string; sub?: string }) {
  return (
    <div className="rounded-xl border bg-card p-5 shadow-sm">
      <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">{label}</p>
      <p className="text-2xl font-bold tabular-nums mt-1">{value}</p>
      {sub && <p className="text-xs text-muted-foreground mt-1">{sub}</p>}
    </div>
  );
}

type SparkMetric = "orders" | "units" | "uplift";

function toPoints(series: DailyMetric[], metric: SparkMetric) {
  return series.map((d) => ({
    date: d.date,
    value: metric === "orders" ? d.orders : metric === "units" ? d.units : d.uplift,
  }));
}

function DiscountDetailContent({ id }: { id: number }) {
  const t = useTranslations("discount.metrics");
  const router = useRouter();
  const [activeMetric, setActiveMetric] = useState<SparkMetric>("orders");

  const { data: campaign, isLoading: loadingCampaign } = useQuery({
    queryKey: ["discount", id],
    queryFn: () => fetchDiscountById(id),
  });

  const { data: metrics, isLoading: loadingMetrics } = useQuery({
    queryKey: ["discount-metrics", id],
    queryFn: () => fetchDiscountMetrics(id),
    enabled: !!campaign,
  });

  const { data: analyticsConfig } = useQuery({
    queryKey: ["analytics-config"],
    queryFn: fetchAnalyticsConfig,
    staleTime: 60 * 60_000,
  });

  const isLoading = loadingCampaign || loadingMetrics;

  const showCutoffBanner =
    campaign && analyticsConfig
      ? campaign.validFrom < analyticsConfig.startDate
      : false;

  const formatDate = (iso: string) =>
    new Date(iso).toLocaleDateString(undefined, { day: "2-digit", month: "short", year: "numeric" });

  const fmtCurrency = (v: number) =>
    v.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="sm" onClick={() => router.back()}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        {loadingCampaign ? (
          <Skeleton className="h-7 w-52" />
        ) : campaign ? (
          <>
            <div
              className="h-4 w-4 rounded-full shrink-0"
              style={{ backgroundColor: `#${campaign.colorHex}` }}
            />
            <h1 className="text-2xl font-semibold">{campaign.title}</h1>
            <span className="text-sm text-muted-foreground">
              {formatDate(campaign.validFrom)} – {formatDate(campaign.validUpto)}
            </span>
            <div className="ml-auto">
              <Button variant="outline" size="sm" asChild>
                <Link href={`/manage/discounts/${id}/edit`}>
                  <Edit2 className="h-4 w-4 mr-2" />
                  {t("editCampaign")}
                </Link>
              </Button>
            </div>
          </>
        ) : null}
      </div>

      {/* Cutoff banner */}
      {showCutoffBanner && analyticsConfig && (
        <div className="flex items-start gap-3 rounded-xl border border-yellow-300 bg-yellow-50 p-4 text-sm text-yellow-800">
          <AlertTriangle className="h-5 w-5 shrink-0 mt-0.5 text-yellow-600" />
          <p>
            {t("cutoffBanner", {
              date: campaign ? formatDate(campaign.validFrom) : "—",
              startDate: new Date(analyticsConfig.startDate).toLocaleDateString(undefined, {
                day: "2-digit",
                month: "short",
                year: "numeric",
              }),
            })}
          </p>
        </div>
      )}

      {/* KPI cards */}
      {isLoading ? (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map((i) => (
            <Skeleton key={i} className="h-24 rounded-xl" />
          ))}
        </div>
      ) : metrics ? (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <KpiCard label={t("ordersUsing")} value={metrics.ordersUsing.toLocaleString()} />
          <KpiCard label={t("unitsMoved")} value={metrics.unitsMoved.toLocaleString()} />
          <KpiCard label={t("revenueUplift")} value={fmtCurrency(metrics.revenueUplift)} sub="TJS" />
          <KpiCard label={t("avgDiscountPerOrder")} value={fmtCurrency(metrics.avgDiscountPerOrder)} sub="TJS" />
        </div>
      ) : null}

      {/* Sparkline panel */}
      {metrics && metrics.dailySeries.length > 0 && (
        <div className="rounded-xl border bg-card p-5 shadow-sm space-y-4">
          <div className="flex items-center gap-2">
            {(["orders", "units", "uplift"] as SparkMetric[]).map((m) => (
              <button
                key={m}
                onClick={() => setActiveMetric(m)}
                className={`px-3 py-1 rounded-full text-xs font-medium transition-colors ${
                  activeMetric === m
                    ? "bg-primary text-white"
                    : "bg-muted text-muted-foreground hover:bg-muted/70"
                }`}
              >
                {t(`sparkline.${m}`)}
              </button>
            ))}
            <span className="ml-auto text-xs text-muted-foreground">
              {t("window", { from: metrics.from, to: metrics.to })}
            </span>
          </div>
          <MetricSparkline
            points={toPoints(metrics.dailySeries, activeMetric)}
            height={100}
          />
        </div>
      )}

      {/* Empty state */}
      {!isLoading && metrics && metrics.ordersUsing === 0 && (
        <div className="flex flex-col items-center justify-center border border-dashed rounded-xl p-12 text-center gap-3">
          <AlertTriangle className="h-10 w-10 text-muted-foreground/40" />
          <p className="text-sm text-muted-foreground">{t("empty")}</p>
        </div>
      )}
    </div>
  );
}

export default function DiscountDetailPage() {
  const { id } = useParams<{ id: string }>();
  return (
    <ProtectedRoute requiredRole="MANAGER">
      <DiscountDetailContent id={Number(id)} />
    </ProtectedRoute>
  );
}
