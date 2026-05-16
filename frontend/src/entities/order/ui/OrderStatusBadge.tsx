"use client";

import { useTranslations } from "next-intl";
import { cn } from "@/shared/lib/utils";

const STATUS_STYLES: Record<string, string> = {
  PAID:      "bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200",
  PENDING:   "bg-amber-50  text-amber-700   ring-1 ring-amber-200",
  DELIVERED: "bg-blue-50   text-blue-700    ring-1 ring-blue-200",
  CANCELLED: "bg-rose-50   text-rose-700    ring-1 ring-rose-200",
  REFUNDED:  "bg-violet-50 text-violet-700  ring-1 ring-violet-200",
  SHIPPED:              "bg-cyan-50   text-cyan-700    ring-1 ring-cyan-200",
  READY_FOR_PICKUP:     "bg-indigo-50 text-indigo-700  ring-1 ring-indigo-200",
  OUT_FOR_DELIVERY:     "bg-blue-50   text-blue-700    ring-1 ring-blue-200",
  DELIVERY_ATTEMPTED:   "bg-amber-50  text-amber-700   ring-1 ring-amber-200",
};

export function OrderStatusBadge({ status }: { status: string }) {
  const t = useTranslations("manage.orders.status");
  const style = STATUS_STYLES[status] ?? "bg-zinc-100 text-zinc-600 ring-1 ring-zinc-200";
  const label = STATUS_STYLES[status]
    ? t(status as Parameters<typeof t>[0])
    : status;

  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide",
        style
      )}
    >
      {label}
    </span>
  );
}
