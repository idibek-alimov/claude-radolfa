"use client";

import { ShoppingBag, Clock, TrendingUp } from "lucide-react";
import { useAdminOrderSummary } from "@/entities/order";

function formatTjs(amount: number): string {
  return (
    new Intl.NumberFormat("ru-RU", { maximumFractionDigits: 0 }).format(amount) +
    " TJS"
  );
}

interface KpiCardProps {
  icon: React.ElementType;
  label: string;
  value: string | undefined;
  iconBg: string;
  iconColor: string;
}

function KpiCard({ icon: Icon, label, value, iconBg, iconColor }: KpiCardProps) {
  return (
    <div className="rounded-xl border bg-card p-5 shadow-sm flex items-center gap-4 hover:shadow-md hover:-translate-y-0.5 transition-all duration-150">
      <div className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-full ${iconBg}`}>
        <Icon className={`h-5 w-5 ${iconColor}`} />
      </div>
      <div className="min-w-0">
        <p className="text-xs text-muted-foreground font-medium uppercase tracking-wide truncate">
          {label}
        </p>
        {value !== undefined ? (
          <p className="text-2xl font-bold tabular-nums leading-tight">{value}</p>
        ) : (
          <div className="mt-1 h-7 w-20 rounded bg-muted animate-pulse" />
        )}
      </div>
    </div>
  );
}

export function OrderKpiRibbon() {
  const { data } = useAdminOrderSummary();

  return (
    <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
      <KpiCard
        icon={ShoppingBag}
        label="Total Orders"
        value={data?.totalOrders.toLocaleString()}
        iconBg="bg-blue-50"
        iconColor="text-blue-500"
      />
      <KpiCard
        icon={Clock}
        label="Orders Today"
        value={data?.todayOrders.toLocaleString()}
        iconBg="bg-amber-50"
        iconColor="text-amber-500"
      />
      <KpiCard
        icon={TrendingUp}
        label="Revenue Today"
        value={data ? formatTjs(data.revenueToday) : undefined}
        iconBg="bg-emerald-50"
        iconColor="text-emerald-500"
      />
    </div>
  );
}
