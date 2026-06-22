"use client";

import { useEffect } from "react";
import Link from "next/link";
import { ShoppingBag, ChevronLeft } from "lucide-react";
import { useTranslations } from "next-intl";
import { Button } from "@radolfa/shared/ui/button";
import { useCartQuery } from "@/features/cart";
import { CartPageSkeleton } from "./CartPageSkeleton";
import { CartItemList } from "./CartItemList";
import { OrderSummary } from "./OrderSummary";
import { RelatedProducts } from "./RelatedProducts";
// import { FreeShippingBanner } from "./FreeShippingBanner"; // temporarily hidden
import { SellerGroupHeader } from "./SellerGroupHeader";
import { CrownBanner } from "./CrownBanner";
import { StickyCheckoutBar } from "./StickyCheckoutBar";

export function CartPage() {
  const t = useTranslations("cart");
  const { data: cart, isLoading } = useCartQuery();

  const hasOutOfStock = cart?.items.some((i) => !i.inStock) ?? false;
  const crownLine = cart?.items.find((i) => i.mechanism === "LOYALTY");
  const crownTierPercent = crownLine?.discountPercent ?? null;

  // Neutralizes the shared layout's `main { flex: 1 }` (which pins the footer
  // to the viewport bottom on short pages) so the footer follows the cart's
  // content immediately instead of leaving a gap. See `globals.css`.
  useEffect(() => {
    document.body.classList.add("cart-chrome");
    return () => document.body.classList.remove("cart-chrome");
  }, []);

  return (
    <>
      <div className="bg-[#F7F2F6]">
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
            <>
              <div className="flex items-baseline gap-3 mb-5">
                <h1 className="font-extrabold text-xl tracking-tight leading-none">
                  {t("yourBag")}
                </h1>
                <span className="text-[13px] text-ink/55 font-medium">
                  {t("itemCount", { count: cart.itemCount })}
                </span>
              </div>

              <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
                <div className="lg:col-span-2 flex flex-col gap-4">
                  {/* <FreeShippingBanner /> — temporarily hidden */}
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
                </aside>
              </div>

              <RelatedProducts />
            </>
          )}
        </div>
      </div>

      {cart && cart.items.length > 0 && (
        <StickyCheckoutBar cart={cart} hasOutOfStock={hasOutOfStock} />
      )}
    </>
  );
}
