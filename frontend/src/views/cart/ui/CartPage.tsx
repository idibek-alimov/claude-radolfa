"use client";

import Link from "next/link";
import { ShoppingBag } from "lucide-react";
import { useTranslations } from "next-intl";
import { Button } from "@/shared/ui/button";
import { useCartQuery } from "@/features/cart";
import { formatPrice } from "@/shared/lib/format";
import { CartPageSkeleton } from "./CartPageSkeleton";
import { CartItemList } from "./CartItemList";
import { OrderSummary } from "./OrderSummary";
import { RelatedProducts } from "./RelatedProducts";

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
        (() => {
          const hasOutOfStock = cart.items.some((i) => !i.inStock);
          return (
            <>
              <div className="pb-24 lg:pb-0">
                <div className="grid grid-cols-1 lg:grid-cols-[1fr_360px] gap-8 lg:gap-12">
                  <CartItemList items={cart.items} hasOutOfStock={hasOutOfStock} />
                  <OrderSummary cart={cart} hasOutOfStock={hasOutOfStock} />
                </div>
                <RelatedProducts />
              </div>

              {/* Mobile-only sticky checkout bar */}
              <div className="fixed bottom-0 left-0 right-0 z-30 bg-background border-t shadow-lg p-4 flex items-center justify-between lg:hidden">
                <div className="flex flex-col">
                  <span className="text-xs text-muted-foreground">{t("subtotal")}</span>
                  <span className="text-lg font-bold tabular-nums">
                    {formatPrice(cart.totalAmount)}
                  </span>
                </div>
                <Button asChild size="lg" disabled={hasOutOfStock}>
                  <Link href="/checkout">{t("proceedToCheckout")}</Link>
                </Button>
              </div>
            </>
          );
        })()
      )}
    </div>
  );
}
