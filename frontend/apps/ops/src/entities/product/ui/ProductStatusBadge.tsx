"use client";

import { useTranslations } from "next-intl";
import { cn } from "@radolfa/shared/lib/utils";
import type { ProductStatus } from "@/entities/product/model/types";

const STATUS_STYLES: Record<string, string> = {
  DRAFT:           "bg-slate-50   text-slate-700   ring-1 ring-slate-200",
  PENDING_REVIEW:  "bg-amber-50   text-amber-700   ring-1 ring-amber-200",
  AWAITING_STOCK:  "bg-violet-50  text-violet-700  ring-1 ring-violet-200",
  ACTIVE:          "bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200",
  REJECTED:        "bg-rose-50    text-rose-700    ring-1 ring-rose-200",
};

export function ProductStatusBadge({ status }: { status: ProductStatus | string }) {
  const t = useTranslations("manage.products.status");
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
