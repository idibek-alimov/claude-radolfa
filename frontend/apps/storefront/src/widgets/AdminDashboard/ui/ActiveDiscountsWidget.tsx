"use client";

import { useQuery } from "@tanstack/react-query";
import { Tag, AlertTriangle, ExternalLink } from "lucide-react";
import Link from "next/link";
import { fetchDiscounts } from "@/features/discount-management/api";
import { cn } from "@/shared/lib";
import type { DiscountResponse } from "@/features/discount-management/model/types";

function hexToRgba(hex: string, alpha: number): string {
  const r = parseInt(hex.slice(0, 2), 16);
  const g = parseInt(hex.slice(2, 4), 16);
  const b = parseInt(hex.slice(4, 6), 16);
  return `rgba(${r}, ${g}, ${b}, ${alpha})`;
}

function formatExpiry(isoString: string): { label: string; daysLeft: number } {
  const now = new Date();
  const expiry = new Date(isoString);
  const diffMs = expiry.getTime() - now.getTime();
  const daysLeft = Math.ceil(diffMs / (1000 * 60 * 60 * 24));

  if (daysLeft < 0) return { label: "Expired", daysLeft };
  if (daysLeft === 0) return { label: "Today", daysLeft };
  if (daysLeft === 1) return { label: "Tomorrow", daysLeft };
  return { label: `${daysLeft}d left`, daysLeft };
}

function DiscountRow({ discount }: { discount: DiscountResponse }) {
  const { label: expiryLabel, daysLeft } = formatExpiry(discount.validUpto);
  const isUrgent = daysLeft >= 0 && daysLeft <= 3;
  const isExpired = daysLeft < 0;

  return (
    <div
      className={cn(
        "flex items-center gap-4 px-4 py-3 rounded-lg transition-colors",
        isUrgent && "bg-amber-50 hover:bg-amber-100/70",
        !isUrgent && !isExpired && "hover:bg-zinc-50/80",
        isExpired && "opacity-40 hover:bg-zinc-50/50"
      )}
    >
      {/* Color dot */}
      <div
        className="h-2.5 w-2.5 shrink-0 rounded-full"
        style={{ backgroundColor: `#${discount.colorHex}` }}
      />

      {/* Campaign name + type */}
      <div className="flex-1 min-w-0">
        <p className="text-sm font-semibold text-zinc-800 truncate">{discount.title}</p>
        <p className="text-[11px] text-zinc-400 truncate">{discount.type.name}</p>
      </div>

      {/* Discount badge */}
      <div
        className="shrink-0 rounded-md px-2 py-0.5 text-xs font-bold"
        style={{
          backgroundColor: hexToRgba(discount.colorHex, 0.12),
          color: `#${discount.colorHex}`,
        }}
      >
        −{discount.amountValue}{discount.amountType === "FIXED" ? " TJS" : "%"}
      </div>

      {/* SKU count */}
      <div className="shrink-0 text-xs text-zinc-400 tabular-nums w-16 text-right">
        {discount.targets.length} SKU{discount.targets.length !== 1 ? "s" : ""}
      </div>

      {/* Expiry */}
      <div
        className={cn(
          "shrink-0 flex items-center justify-end gap-1 text-xs font-medium w-28",
          isUrgent ? "text-amber-600" : "text-zinc-400"
        )}
      >
        {isUrgent && <AlertTriangle className="h-3 w-3 shrink-0" />}
        {expiryLabel}
      </div>
    </div>
  );
}

export function ActiveDiscountsWidget() {
  const { data, isLoading } = useQuery({
    queryKey: ["discounts-active-dashboard"],
    queryFn: () => fetchDiscounts({ status: "active", page: 1, size: 20 }),
    staleTime: 60_000,
  });

  const discounts = data?.content ?? [];

  return (
    <div>
      <div className="flex items-center justify-between mb-3">
        <h2 className="text-[11px] font-semibold uppercase tracking-widest text-zinc-400">
          Active Campaigns
        </h2>
        <Link
          href="/manage/discounts"
          className="flex items-center gap-1 text-xs font-medium text-zinc-400 hover:text-zinc-700 transition-colors"
        >
          View all
          <ExternalLink className="h-3 w-3" />
        </Link>
      </div>

      <div className="bg-white rounded-xl border border-zinc-100 shadow-sm overflow-hidden">
        {/* Column headers */}
        <div className="flex items-center gap-4 px-4 py-2.5 border-b border-zinc-100 bg-zinc-50/60">
          <div className="w-2.5 shrink-0" />
          <div className="flex-1 text-[10px] font-bold uppercase tracking-wider text-zinc-400">
            Campaign
          </div>
          <div className="w-12 text-[10px] font-bold uppercase tracking-wider text-zinc-400 text-right">
            Disc.
          </div>
          <div className="w-16 text-[10px] font-bold uppercase tracking-wider text-zinc-400 text-right">
            SKUs
          </div>
          <div className="w-28 text-[10px] font-bold uppercase tracking-wider text-zinc-400 text-right">
            Expires
          </div>
        </div>

        {/* Rows */}
        {isLoading ? (
          <div className="p-4 space-y-2">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-12 rounded-lg bg-zinc-100 animate-pulse" />
            ))}
          </div>
        ) : discounts.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-14 gap-2 text-zinc-400">
            <Tag className="h-8 w-8 text-zinc-200" strokeWidth={1.5} />
            <p className="text-sm font-semibold text-zinc-400">No active campaigns</p>
            <p className="text-xs text-zinc-300">Create a discount to run a promotion.</p>
          </div>
        ) : (
          <div className="px-1 py-1">
            {discounts.map((d) => (
              <DiscountRow key={d.id} discount={d} />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
