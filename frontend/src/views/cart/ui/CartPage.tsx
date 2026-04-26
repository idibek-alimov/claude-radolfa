"use client";

import Link from "next/link";
import { ShoppingBag } from "lucide-react";
import { useTranslations } from "next-intl";
import { Button } from "@/shared/ui/button";
import { useCartQuery } from "@/features/cart";
import { CartPageSkeleton } from "./CartPageSkeleton";

export function CartPage() {
  const t = useTranslations("cart");
  const { data: cart, isLoading } = useCartQuery();

  return (
    <div className="max-w-[1400px] mx-auto px-4 sm:px-6 lg:px-8 py-8 lg:py-12">
      <h1 className="text-3xl font-bold tracking-tight mb-8">{t("cartHeading")}</h1>

      {isLoading ? (
        <CartPageSkeleton />
      ) : !cart || cart.items.length === 0 ? (
        <div className="border border-dashed rounded-xl p-12 flex flex-col items-center gap-4 text-center">
          <ShoppingBag className="h-10 w-10 text-muted-foreground/40" />
          <div className="space-y-1">
            <p className="text-base font-medium">{t("empty")}</p>
            <p className="text-sm text-muted-foreground">{t("emptyDescription")}</p>
          </div>
          <Button asChild variant="outline">
            <Link href="/products">{t("browseProducts")}</Link>
          </Button>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-[1fr_360px] gap-8 lg:gap-12">
          {/* Left column — Phase 3 replaces this block with <CartItemList items={cart.items} /> */}
          <ul className="divide-y rounded-xl border bg-card">
            {cart.items.map((item) => (
              <li key={item.skuId} className="p-4 flex items-center justify-between gap-4">
                <div className="min-w-0">
                  <p className="font-medium truncate">{item.productName}</p>
                  <p className="text-sm text-muted-foreground">
                    {item.colorName} · {item.sizeLabel} · ×{item.quantity}
                  </p>
                </div>
                <p className="font-semibold tabular-nums flex-shrink-0">
                  {item.lineTotal.toLocaleString()} TJS
                </p>
              </li>
            ))}
          </ul>

          {/* Right column — Phase 3 replaces this block with <OrderSummary cart={cart} /> */}
          <div className="lg:sticky lg:top-24">
            <div className="rounded-xl border bg-card p-6 space-y-4">
              <h2 className="text-base font-semibold">{t("orderSummary")}</h2>
              <div className="flex items-center justify-between text-sm">
                <span className="text-muted-foreground">{t("subtotal")}</span>
                <span className="font-semibold tabular-nums">
                  {cart.totalAmount.toLocaleString()} TJS
                </span>
              </div>
              <Button asChild className="w-full">
                <Link href="/checkout">{t("proceedToCheckout")}</Link>
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
