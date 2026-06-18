"use client";

import Link from "next/link";
import { ShoppingBag, ChevronLeft } from "lucide-react";
import { useTranslations } from "next-intl";
import { Button } from "@radolfa/shared/ui/button";
import { useCartQuery } from "@/features/cart";
import { CartPageSkeleton } from "./CartPageSkeleton";
import { CartItemList } from "./CartItemList";
import { OrderSummary } from "./OrderSummary";
import { RelatedProducts } from "./RelatedProducts";
import { FreeShippingBanner } from "./FreeShippingBanner";
import { SellerGroupHeader } from "./SellerGroupHeader";
import { CrownBanner } from "./CrownBanner";
import { TrustRow } from "./TrustRow";
import { StickyCheckoutBar } from "./StickyCheckoutBar";

export function CartPage() {
  const t = useTranslations("cart");
  const { data: cart, isLoading } = useCartQuery();

  return (
    <div className="max-w-[1440px] mx-auto px-4 sm:px-6 py-6">
      {isLoading ? (
        <CartPageSkeleton />
      ) : !cart || cart.items.length === 0 ? (
        <div className="border border-dashed border-ink/15 rounded-2xl p-12 flex flex-col items-center gap-4 text-center">
          <ShoppingBag className="h-10 w-10 text-ink/30" />
          <div className="space-y-1">
            <p className="text-base font-medium">{t("empty")}</p>
            <p className="text-sm text-ink/55">{t("emptyDescription")}</p>
          </div>
          <Button asChild variant="outline">
            <Link href="/search">{t("browseProducts")}</Link>
          </Button>
        </div>
      ) : (
        (() => {
          const hasOutOfStock = cart.items.some((i) => !i.inStock);
          const crownLine = cart.items.find((i) => i.mechanism === "LOYALTY");
          const crownTierPercent = crownLine?.discountPercent ?? null;

          return (
            <>
              <div className="flex items-baseline gap-3 mb-5">
                <h1 className="font-black text-[28px] sm:text-[34px] leading-none">
                  {t("yourBag")}
                </h1>
                <span className="text-[14px] sm:text-[15px] text-ink/55 font-medium">
                  {t("itemCount", { count: cart.itemCount })}
                </span>
              </div>

              <div className="pb-40 md:pb-28 lg:pb-0">
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
                  <div className="lg:col-span-2 flex flex-col gap-4">
                    <FreeShippingBanner />
                    <SellerGroupHeader />
                    <CartItemList items={cart.items} hasOutOfStock={hasOutOfStock} />
                    <Link
                      href="/search"
                      className="inline-flex items-center gap-2 text-[14px] font-semibold text-mag hover:text-maglo mt-1 w-fit"
                    >
                      <ChevronLeft className="h-4 w-4" />
                      {t("continueShopping")}
                    </Link>
                  </div>

                  <aside className="lg:sticky lg:top-36 lg:self-start flex flex-col gap-4">
                    {crownTierPercent != null && <CrownBanner percent={crownTierPercent} />}
                    <OrderSummary
                      cart={cart}
                      hasOutOfStock={hasOutOfStock}
                      crownTierPercent={crownTierPercent}
                    />
                    <TrustRow />
                  </aside>
                </div>
                <RelatedProducts />
              </div>

              <StickyCheckoutBar cart={cart} hasOutOfStock={hasOutOfStock} />
            </>
          );
        })()
      )}
    </div>
  );
}
