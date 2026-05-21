"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { OrderStatusBadge } from "@/entities/order";
import { formatDate, formatPrice } from "@/shared/lib/format";
import type { Order } from "@/features/profile/types";

export function OrderHistoryCard({ order }: { order: Order }) {
  const t = useTranslations("profile");
  return (
    <div className="flex items-center justify-between rounded-xl border bg-card px-5 py-4">
      <div className="space-y-1">
        <div className="flex items-center gap-2">
          <span className="text-sm font-medium">
            {t("orderNumber", { id: order.id })}
          </span>
          <OrderStatusBadge status={order.status} namespace="profile.status" />
        </div>
        <p className="text-xs text-muted-foreground">
          {formatDate(order.createdAt)} ·{" "}
          {t("itemCount", { count: order.items.length })} ·{" "}
          {formatPrice(order.totalAmount)}
        </p>
      </div>
      <Link
        href={`/orders/${order.id}`}
        className="text-xs text-primary hover:underline shrink-0"
      >
        {t("viewDetails")} →
      </Link>
    </div>
  );
}
