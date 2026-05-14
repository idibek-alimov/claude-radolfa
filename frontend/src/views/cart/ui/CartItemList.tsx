"use client";

import { AlertTriangle } from "lucide-react";
import { useTranslations } from "next-intl";
import { CartItemRow } from "@/features/cart";
import type { CartItem } from "@/entities/cart";

interface CartItemListProps {
  items: CartItem[];
  hasOutOfStock: boolean;
}

export function CartItemList({ items, hasOutOfStock }: CartItemListProps) {
  const t = useTranslations("cart");

  return (
    <div className="space-y-4">
      {hasOutOfStock && (
        <div className="rounded-xl border border-destructive/30 bg-destructive/5 p-4 flex items-start gap-3">
          <AlertTriangle className="h-5 w-5 text-destructive shrink-0 mt-0.5" />
          <p className="text-sm text-destructive">{t("outOfStockWarning")}</p>
        </div>
      )}
      <ul className="divide-y rounded-xl border bg-card px-4">
        {items.map((item) => (
          <li key={item.skuId}>
            <CartItemRow item={item} />
          </li>
        ))}
      </ul>
    </div>
  );
}
