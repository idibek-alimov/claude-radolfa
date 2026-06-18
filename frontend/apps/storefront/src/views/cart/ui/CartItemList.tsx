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
    <div className="flex flex-col gap-3 sm:gap-4">
      {hasOutOfStock && (
        <div className="rounded-2xl border border-sale/30 bg-sale/5 p-4 flex items-start gap-3">
          <AlertTriangle className="h-5 w-5 text-sale shrink-0 mt-0.5" />
          <p className="text-sm text-sale">{t("outOfStockWarning")}</p>
        </div>
      )}
      {items.map((item) => (
        <CartItemRow key={item.skuId} item={item} />
      ))}
    </div>
  );
}
