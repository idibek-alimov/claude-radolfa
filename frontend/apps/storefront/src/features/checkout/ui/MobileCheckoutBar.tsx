"use client";

import type { ReactNode } from "react";
import { useTranslations } from "next-intl";
import { formatPrice } from "@radolfa/shared/lib/format";
import type { useCheckout } from "../hooks/useCheckout";

interface MobileCheckoutBarProps {
  checkout: ReturnType<typeof useCheckout>;
}

export function MobileCheckoutBar({ checkout }: MobileCheckoutBarProps) {
  const t = useTranslations("checkout");
  const tc = useTranslations("cart");

  const {
    step,
    cart,
    paymentMethod,
    codHandlingFee,
    pointsToRedeem,
    deliveryInvalid,
    placeOrder,
    isPlacing,
    hasOutOfStockItems,
    next,
  } = checkout;

  if (!cart || step === "done") return null;

  const codFee = paymentMethod === "COD" ? codHandlingFee : 0;
  const estimatedTotal = Math.max(0, cart.totalAmount - pointsToRedeem * 0.01 + codFee);
  const total = formatPrice(estimatedTotal);

  let label: ReactNode = null;
  let onClick = next;
  let disabled = false;

  if (step === "delivery") {
    label = t("delivery.continueToReview");
    disabled = deliveryInvalid;
  } else if (step === "review") {
    label = t("review.continueToPayment");
  } else {
    onClick = () => placeOrder();
    disabled = isPlacing || hasOutOfStockItems;
    label = isPlacing
      ? t("placing")
      : paymentMethod === "CARD"
        ? t("payment.payCta", { total })
        : t("payment.placeOrderCta");
  }

  return (
    <div className="lg:hidden fixed bottom-0 left-0 right-0 z-30 bg-white border-t border-ink/10 px-4 pt-3 pb-4">
      <div className="flex items-center justify-between mb-2.5">
        <div>
          <div className="text-[11px] text-ink/55">{tc("total")}</div>
          <div className="font-black text-[20px] tabular-nums leading-none mt-0.5">{total}</div>
        </div>
        {cart.savings > 0 && (
          <div className="text-[11px] text-emerald font-semibold">
            {tc("youSave", { amount: formatPrice(cart.savings) })}
          </div>
        )}
      </div>
      <button
        onClick={onClick}
        disabled={disabled}
        className="w-full h-12 rounded-full bg-mag text-white font-bold text-[15px] inline-flex items-center justify-center gap-2 shadow-lg shadow-mag/20 disabled:opacity-50"
      >
        {label}
      </button>
    </div>
  );
}
