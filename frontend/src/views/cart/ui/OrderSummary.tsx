"use client";

import Link from "next/link";
import { Trash2, BadgeCheck } from "lucide-react";
import { useTranslations } from "next-intl";
import { Button } from "@/shared/ui/button";
import { useClearCart } from "@/features/cart";
import { formatPrice } from "@/shared/lib/format";
import type { Cart } from "@/entities/cart";

interface OrderSummaryProps {
  cart: Cart;
  hasOutOfStock: boolean;
}

export function OrderSummary({ cart, hasOutOfStock }: OrderSummaryProps) {
  const t = useTranslations("cart");
  const clearCart = useClearCart();

  return (
    <div className="lg:sticky lg:top-24">
      <div className="rounded-xl border bg-card p-6 space-y-4">
        <h2 className="text-base font-semibold">{t("orderSummary")}</h2>

        <div className="flex items-center justify-between text-sm">
          <span className="text-muted-foreground">
            {t("itemCount", { count: cart.itemCount })}
          </span>
        </div>

        {cart.couponCode && (
          <div className="flex items-center gap-2 rounded-lg border bg-muted/40 p-2 text-xs">
            <BadgeCheck className="h-4 w-4 text-primary shrink-0" />
            <span className="font-medium">{cart.couponCode}</span>
          </div>
        )}

        <div className="border-t pt-4 flex items-center justify-between">
          <span className="text-sm font-semibold">{t("subtotal")}</span>
          <span className="text-xl font-bold tabular-nums">
            {formatPrice(cart.totalAmount)}
          </span>
        </div>

        <Button asChild className="w-full" disabled={hasOutOfStock}>
          <Link href="/checkout">{t("proceedToCheckout")}</Link>
        </Button>

        <button
          onClick={() => clearCart.mutate()}
          disabled={clearCart.isPending}
          className="w-full flex items-center justify-center gap-1.5 text-xs text-muted-foreground hover:text-destructive transition-colors py-1"
        >
          <Trash2 className="h-3.5 w-3.5" />
          {t("clearCart")}
        </button>
      </div>
    </div>
  );
}
