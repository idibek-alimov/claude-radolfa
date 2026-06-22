"use client";

import Link from "next/link";
import { ShieldCheck } from "lucide-react";
import { useTranslations } from "next-intl";
import { formatPrice } from "@radolfa/shared/lib/format";
import { useHideOnScroll } from "@radolfa/shared/lib/useHideOnScroll";
import type { Cart } from "@/entities/cart";

interface StickyCheckoutBarProps {
  cart: Cart;
  hasOutOfStock: boolean;
}

/** Sits directly above `BottomNav` (`bottom-14`) and uses the same
 * `useHideOnScroll` defaults so both bars slide away/reveal together. */
export function StickyCheckoutBar({ cart, hasOutOfStock }: StickyCheckoutBarProps) {
  const t = useTranslations("cart");
  const hidden = useHideOnScroll();

  return (
    <div
      className={`fixed bottom-14 md:bottom-0 left-0 right-0 z-30 bg-white border-t border-ink/10 px-4 pt-3 pb-4 lg:hidden transition-transform duration-300 ${
        hidden ? "translate-y-[calc(100%+3.5rem)] md:translate-y-full" : "translate-y-0"
      }`}
    >
      <div className="flex items-center justify-between mb-2.5">
        <div>
          <div className="text-[11px] text-ink/55">
            {t("total")}
            {cart.savings > 0 && ` · ${t("youSave", { amount: formatPrice(cart.savings) })}`}
          </div>
          <div className="font-black text-[22px] tabular-nums leading-none mt-0.5">
            {formatPrice(cart.totalAmount)}
          </div>
        </div>
        <div className="flex items-center gap-1.5 text-[11px] text-ink/55">
          <ShieldCheck className="h-[13px] w-[13px]" strokeWidth={2} />
          {t("securePayment")}
        </div>
      </div>
      <Link
        href="/checkout"
        aria-disabled={hasOutOfStock}
        className={`w-full h-12 rounded-full bg-mag text-white font-bold text-[15px] inline-flex items-center justify-center gap-2 shadow-lg shadow-mag/20 ${hasOutOfStock ? "opacity-50 pointer-events-none" : "hover:bg-maglo"}`}
      >
        {t("proceedToCheckout")}
      </Link>
    </div>
  );
}
