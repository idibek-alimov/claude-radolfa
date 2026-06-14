"use client";

import Link from "next/link";
import Image from "next/image";
import { useTranslations } from "next-intl";
import { OrderStatusBadge } from "@/entities/order";
import { formatDate, formatPrice } from "@radolfa/shared/lib/format";
import type { Order } from "@/features/profile/types";

/**
 * Compact recent-order row for the Overview dashboard — verbatim Tailwind
 * from the B-Magenta `<!-- OVERVIEW -->` "Recent orders" article (desktop
 * `w-16 h-16`, mobile `w-14 h-14`), merged into one responsive component.
 * Reuses `OrderStatusBadge` (`profile.status` namespace) for the status pill.
 */
export function OverviewOrderRow({ order }: { order: Order }) {
  const t = useTranslations("profile");
  const firstItem = order.items[0];

  const isDelivered = order.status === "DELIVERED";
  const canBuyAgain = isDelivered && !!firstItem?.slug;

  return (
    <article className="flex items-center gap-3 lg:gap-4">
      <div className="w-14 h-14 lg:w-16 lg:h-16 rounded-2xl bg-plum overflow-hidden shrink-0 relative">
        {firstItem?.imageUrl && (
          <Image
            src={firstItem.imageUrl}
            alt={firstItem.productName}
            fill
            className="object-cover"
            unoptimized
          />
        )}
      </div>
      <div className="min-w-0 flex-1">
        <div className="text-[12px] lg:text-[13px] font-bold truncate">
          {firstItem?.productName}
          {order.items.length > 1 ? ` +${order.items.length - 1}` : ""}
        </div>
        <div className="hidden lg:block text-[11px] text-ink/50 font-mono">
          {t("orderNumber", { id: order.id })} · {formatDate(order.createdAt)}
        </div>
        <span className="mt-1 lg:mt-1 inline-flex">
          <OrderStatusBadge status={order.status} namespace="profile.status" />
        </span>
      </div>
      <div className="text-right shrink-0">
        <div className="font-black tabular-nums text-mag text-[13px] lg:text-base">
          {formatPrice(order.totalAmount)}
        </div>
        {canBuyAgain ? (
          <Link
            href={`/products/${firstItem!.slug}`}
            className="mt-1 inline-flex h-7 lg:h-8 px-2.5 lg:px-3 rounded-full bg-mag text-white text-[10px] lg:text-[11px] font-bold items-center"
          >
            {t("buyAgain")}
          </Link>
        ) : (
          <Link
            href={`/orders/${order.id}`}
            className="mt-1 inline-flex h-7 lg:h-8 px-2.5 lg:px-3 rounded-full border-2 border-ink text-[10px] lg:text-[11px] font-bold items-center"
          >
            {t("track")}
          </Link>
        )}
      </div>
    </article>
  );
}
