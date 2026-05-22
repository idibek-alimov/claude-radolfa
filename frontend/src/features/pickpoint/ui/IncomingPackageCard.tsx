"use client";

import { useState } from "react";
import Image from "next/image";
import { useTranslations } from "next-intl";
import { Checkbox } from "@/shared/ui/checkbox";
import { cn } from "@/shared/lib/utils";
import type { PickpointOrder } from "@/entities/user";

const COLLAPSED_ITEM_LIMIT = 3;

interface Props {
  order: PickpointOrder;
  selected: boolean;
  onToggle: (orderId: number) => void;
}

export function IncomingPackageCard({ order, selected, onToggle }: Props) {
  const t = useTranslations("pickpoint");
  const [expanded, setExpanded] = useState(false);
  const visibleItems = expanded
    ? order.items
    : order.items.slice(0, COLLAPSED_ITEM_LIMIT);

  return (
    <div
      className={cn(
        "rounded-xl border bg-card p-4 transition-colors",
        selected && "border-primary/50 bg-primary/5",
      )}
    >
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm font-semibold">
            {t("orderLabel")} #{order.orderId}
          </p>
          <p className="text-xs text-muted-foreground">{order.customerFirstName}</p>
        </div>
        <Checkbox
          checked={selected}
          onCheckedChange={() => onToggle(order.orderId)}
          aria-label={t("selectOrder")}
        />
      </div>

      <div className="mt-2 flex items-center gap-3 text-xs text-muted-foreground">
        <span>
          {order.totalItemCount} {t("items")}
        </span>
        {order.totalWeightKg != null && <span>{order.totalWeightKg} kg</span>}
      </div>

      {order.items.length > 0 && (
        <div className="mt-2 space-y-1">
          {visibleItems.map((item, i) => (
            <div key={i} className="flex items-center gap-2 text-xs">
              {item.imageUrl && (
                <Image
                  src={item.imageUrl}
                  alt={item.productName}
                  width={24}
                  height={24}
                  unoptimized
                  className="rounded object-cover shrink-0"
                />
              )}
              <span className="flex-1 truncate">{item.productName}</span>
              {item.sizeLabel && (
                <span className="shrink-0 text-muted-foreground">
                  {item.sizeLabel}
                </span>
              )}
              <span className="shrink-0">× {item.quantity}</span>
            </div>
          ))}
          {order.items.length > COLLAPSED_ITEM_LIMIT && (
            <button
              className="text-xs text-primary hover:underline"
              onClick={() => setExpanded((v) => !v)}
            >
              {expanded
                ? t("showLess")
                : t("showMore", {
                    count: order.items.length - COLLAPSED_ITEM_LIMIT,
                  })}
            </button>
          )}
        </div>
      )}
    </div>
  );
}
