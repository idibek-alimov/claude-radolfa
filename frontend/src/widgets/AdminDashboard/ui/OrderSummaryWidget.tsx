"use client";

import { useQuery } from "@tanstack/react-query";
import { ShoppingBag, TrendingUp, Calendar, ExternalLink } from "lucide-react";
import Link from "next/link";
import { fetchAdminOrderSummary } from "@/features/admin-dashboard";
import { cn } from "@/shared/lib";
import type { RecentOrder } from "@/features/admin-dashboard";

// ── Utilities ─────────────────────────────────────────────────────────────────

function formatTjs(amount: number): string {
  return (
    new Intl.NumberFormat("ru-RU", { maximumFractionDigits: 0 }).format(amount) +
    "\u00a0TJS"
  );
}

function timeAgo(isoString: string): string {
  const diff = Math.floor((Date.now() - new Date(isoString).getTime()) / 1000);
  if (diff < 60) return `${diff}s ago`;
  if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
  if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
  return `${Math.floor(diff / 86400)}d ago`;
}

// ── Status badge ──────────────────────────────────────────────────────────────

const STATUS_STYLES: Record<string, string> = {
  PAID:      "bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200",
  PENDING:   "bg-amber-50  text-amber-700   ring-1 ring-amber-200",
  DELIVERED: "bg-blue-50   text-blue-700    ring-1 ring-blue-200",
  CANCELLED: "bg-rose-50   text-rose-700    ring-1 ring-rose-200",
  REFUNDED:  "bg-violet-50 text-violet-700  ring-1 ring-violet-200",
  SHIPPED:   "bg-cyan-50   text-cyan-700    ring-1 ring-cyan-200",
};

function StatusBadge({ status }: { status: string }) {
  const style = STATUS_STYLES[status] ?? "bg-zinc-100 text-zinc-600 ring-1 ring-zinc-200";
  return (
    <span className={cn("inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide", style)}>
      {status}
    </span>
  );
}

// ── Stat column ───────────────────────────────────────────────────────────────

interface StatColumnProps {
  icon: React.ElementType;
  label: string;
  value: string | number | undefined;
  accentClass: string;
  iconBg: string;
  iconColor: string;
  isLast?: boolean;
}

function StatColumn({ icon: Icon, label, value, accentClass, iconBg, iconColor, isLast }: StatColumnProps) {
  return (
    <div className={cn("flex-1 flex items-center gap-4 px-6 py-5", !isLast && "border-r border-zinc-100")}>
      <div className={cn("flex h-9 w-9 shrink-0 items-center justify-center rounded-lg", iconBg)}>
        <Icon className={cn("h-4 w-4", iconColor)} strokeWidth={1.75} />
      </div>
      <div className="min-w-0">
        <p className="text-[10px] font-semibold uppercase tracking-widest text-zinc-400 truncate">{label}</p>
        {value !== undefined ? (
          <p className={cn("mt-0.5 text-xl font-bold tabular-nums leading-tight", accentClass)}>
            {value}
          </p>
        ) : (
          <div className="mt-1 h-6 w-24 rounded bg-zinc-100 animate-pulse" />
        )}
      </div>
    </div>
  );
}

// ── Order table row ───────────────────────────────────────────────────────────

function OrderRow({ order, index }: { order: RecentOrder; index: number }) {
  return (
    <div className="grid grid-cols-[2rem_1fr_auto_auto_auto] items-center gap-x-4 px-4 py-2.5 rounded-lg hover:bg-zinc-50/80 transition-colors">
      {/* # */}
      <span className="text-[11px] font-mono text-zinc-300 tabular-nums text-right">
        {String(index + 1).padStart(2, "0")}
      </span>

      {/* Phone */}
      <span className="text-xs font-medium text-zinc-700 truncate font-mono">
        {order.userPhone}
      </span>

      {/* Amount */}
      <span className="text-xs font-semibold tabular-nums text-zinc-800 text-right">
        {formatTjs(order.totalAmount)}
      </span>

      {/* Status */}
      <StatusBadge status={order.status} />

      {/* Time */}
      <span className="text-[11px] text-zinc-400 text-right whitespace-nowrap w-14">
        {timeAgo(order.createdAt)}
      </span>
    </div>
  );
}

// ── Main widget ───────────────────────────────────────────────────────────────

export function OrderSummaryWidget() {
  const { data, isLoading } = useQuery({
    queryKey: ["admin-order-summary"],
    queryFn: fetchAdminOrderSummary,
    staleTime: 60_000,
  });

  return (
    <div>
      <div className="flex items-center justify-between mb-3">
        <h2 className="text-[11px] font-semibold uppercase tracking-widest text-zinc-400">
          Orders & Revenue
        </h2>
      </div>

      <div className="bg-white rounded-xl border border-zinc-100 shadow-sm overflow-hidden">
        {/* ── Top stats strip ─────────────────────────────── */}
        <div className="flex divide-x divide-zinc-100 border-b border-zinc-100">
          <StatColumn
            icon={ShoppingBag}
            label="Total Orders"
            value={isLoading ? undefined : data?.totalOrders.toLocaleString()}
            accentClass="text-blue-600"
            iconBg="bg-blue-50"
            iconColor="text-blue-500"
          />
          <StatColumn
            icon={TrendingUp}
            label="Revenue Today"
            value={isLoading ? undefined : formatTjs(data?.revenueToday ?? 0)}
            accentClass="text-emerald-600"
            iconBg="bg-emerald-50"
            iconColor="text-emerald-500"
          />
          <StatColumn
            icon={Calendar}
            label="This Month"
            value={isLoading ? undefined : formatTjs(data?.revenueThisMonth ?? 0)}
            accentClass="text-violet-600"
            iconBg="bg-violet-50"
            iconColor="text-violet-500"
            isLast
          />
        </div>

        {/* ── Recent orders ────────────────────────────────── */}
        <div>
          {/* Column headers */}
          <div className="grid grid-cols-[2rem_1fr_auto_auto_auto] items-center gap-x-4 px-4 py-2.5 border-b border-zinc-100 bg-zinc-50/60">
            <span />
            <span className="text-[10px] font-bold uppercase tracking-wider text-zinc-400">Phone</span>
            <span className="text-[10px] font-bold uppercase tracking-wider text-zinc-400 text-right">Amount</span>
            <span className="text-[10px] font-bold uppercase tracking-wider text-zinc-400">Status</span>
            <span className="text-[10px] font-bold uppercase tracking-wider text-zinc-400 text-right w-14">When</span>
          </div>

          {/* Rows */}
          {isLoading ? (
            <div className="p-4 space-y-2">
              {[1, 2, 3, 4, 5].map((i) => (
                <div key={i} className="h-9 rounded-lg bg-zinc-100 animate-pulse" />
              ))}
            </div>
          ) : !data?.recentOrders.length ? (
            <div className="flex flex-col items-center justify-center py-14 gap-2 text-zinc-400">
              <ShoppingBag className="h-8 w-8 text-zinc-200" strokeWidth={1.5} />
              <p className="text-sm font-semibold text-zinc-400">No orders yet</p>
              <p className="text-xs text-zinc-300">Orders will appear here once customers check out.</p>
            </div>
          ) : (
            <div className="px-1 py-1">
              {data.recentOrders.map((order, i) => (
                <OrderRow key={order.orderId} order={order} index={i} />
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
