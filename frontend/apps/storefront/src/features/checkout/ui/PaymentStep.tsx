"use client";

import { useTranslations } from "next-intl";
import { cn } from "@radolfa/shared/lib/utils";
import { formatPrice } from "@radolfa/shared/lib/format";
import type { useCheckout } from "../hooks/useCheckout";

interface PaymentStepProps {
  checkout: ReturnType<typeof useCheckout>;
}

export function PaymentStep({ checkout }: PaymentStepProps) {
  const t = useTranslations("checkout");

  const {
    cart,
    paymentMethod,
    setPaymentMethod,
    codHandlingFee,
    placeOrder,
    isPlacing,
    hasOutOfStockItems,
    back,
  } = checkout;

  if (!cart) return null;

  const fee = formatPrice(codHandlingFee);
  const total = formatPrice(cart.totalAmount);

  return (
    <div className="flex flex-col gap-5">
      <div className="bg-white rounded-2xl border border-ink/8 p-4 md:p-6">
        <h2 className="font-black text-[15px] md:text-[19px] mb-1">{t("payment.title")}</h2>
        <p className="text-[12px] md:text-[13px] text-ink/55 mb-3 md:mb-4">
          {t("payment.subtitle")}
        </p>

        <div className="flex flex-col gap-2.5 md:gap-3">
          {/* Card */}
          <label
            className={cn(
              "seg rounded-xl border-2 p-3 md:p-4 flex items-center gap-3 cursor-pointer",
              paymentMethod === "CARD"
                ? "border-mag bg-mag/5"
                : "border-ink/10 hover:border-ink/25"
            )}
          >
            <input
              type="radio"
              name="paymentMethod"
              className="w-4 h-4 accent-mag"
              checked={paymentMethod === "CARD"}
              onChange={() => setPaymentMethod("CARD")}
            />
            <svg
              width="22"
              height="22"
              viewBox="0 0 24 24"
              fill="none"
              stroke={paymentMethod === "CARD" ? "#CB11AB" : "currentColor"}
              strokeWidth="2"
              className="shrink-0"
            >
              <rect x="2" y="5" width="20" height="14" rx="2" />
              <path d="M2 10h20" />
            </svg>
            <div className="flex-1 min-w-0">
              <div className="text-[13px] md:text-[14px] font-bold inline-flex items-center gap-2">
                {t("payment.card.title")}
                <span className="px-1.5 py-0.5 rounded bg-ink/8 text-[9px] md:text-[10px] font-bold text-ink/45 inline-flex items-center gap-1">
                  <svg width="8" height="8" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3">
                    <path d="M7 17 17 7M9 7h8v8" />
                  </svg>
                  {t("payment.card.redirect")}
                </span>
              </div>
              <div className="text-[11px] md:text-[12px] text-ink/55">
                {t("payment.card.subtitle")}
              </div>
            </div>
            <div className="hidden sm:flex gap-1.5 text-[10px] font-bold text-ink/40 shrink-0">
              <span className="px-2 py-1 rounded bg-ink/5">VISA</span>
              <span className="px-2 py-1 rounded bg-ink/5">MC</span>
              <span className="px-2 py-1 rounded bg-ink/5">Korti Milli</span>
            </div>
          </label>

          {/* COD */}
          <label
            className={cn(
              "seg rounded-xl border-2 p-3 md:p-4 flex items-center gap-3 cursor-pointer",
              paymentMethod === "COD"
                ? "border-mag bg-mag/5"
                : "border-ink/10 hover:border-ink/25"
            )}
          >
            <input
              type="radio"
              name="paymentMethod"
              className="w-4 h-4 accent-mag"
              checked={paymentMethod === "COD"}
              onChange={() => setPaymentMethod("COD")}
            />
            <svg
              width="22"
              height="22"
              viewBox="0 0 24 24"
              fill="none"
              stroke={paymentMethod === "COD" ? "#CB11AB" : "currentColor"}
              strokeWidth="2"
              className="shrink-0"
            >
              <circle cx="12" cy="12" r="9" />
              <path d="M12 7v10" />
              <path d="M9.5 9.5h3.5a2 2 0 0 1 0 4H10" />
            </svg>
            <div className="flex-1 min-w-0">
              <div className="text-[13px] md:text-[14px] font-bold">{t("payment.cod.title")}</div>
              <div className="text-[11px] md:text-[12px] text-ink/55">
                {t("payment.cod.subtitle", { fee })}
              </div>
            </div>
          </label>
        </div>

        {/* Reassurance notes */}
        {paymentMethod === "CARD" ? (
          <div className="mt-3 md:mt-4 flex items-start gap-2.5 md:gap-3 rounded-xl bg-soft/60 border border-ink/8 p-3 md:p-4">
            <svg
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="#1F8A5B"
              strokeWidth="2"
              className="mt-0.5 shrink-0"
            >
              <rect x="3" y="11" width="18" height="11" rx="2" />
              <path d="M7 11V7a5 5 0 0 1 10 0v4" />
            </svg>
            <div className="text-[12px] md:text-[13px] text-ink/70 leading-relaxed">
              {t("payment.redirectNote")}{" "}
              <span className="text-ink/50">{t("payment.redirectNoteSub")}</span>
            </div>
          </div>
        ) : (
          <div className="mt-3 md:mt-4 flex items-start gap-2.5 md:gap-3 rounded-xl bg-soft/60 border border-ink/8 p-3 md:p-4">
            <svg
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="#9A6E0F"
              strokeWidth="2"
              className="mt-0.5 shrink-0"
            >
              <circle cx="12" cy="12" r="9" />
              <path d="M12 7v10" />
              <path d="M9.5 9.5h3.5a2 2 0 0 1 0 4H10" />
            </svg>
            <div className="text-[12px] md:text-[13px] text-ink/70 leading-relaxed">
              {t("payment.codNote", { fee })}
            </div>
          </div>
        )}
      </div>

      {/* Footer */}
      <div className="flex items-center justify-between">
        <button
          onClick={back}
          className="text-[14px] font-semibold text-mag hover:text-maglo inline-flex items-center gap-1.5"
        >
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2">
            <path d="M19 12H5" />
            <path d="m12 19-7-7 7-7" />
          </svg>
          {t("payment.backToReview")}
        </button>
        <button
          onClick={() => placeOrder()}
          disabled={isPlacing || hasOutOfStockItems}
          className="h-12 px-8 md:px-9 rounded-full bg-mag text-white font-bold text-[15px] hover:bg-maglo transition inline-flex items-center gap-2 shadow-lg shadow-mag/20 disabled:opacity-50"
        >
          {isPlacing ? (
            t("placing")
          ) : paymentMethod === "CARD" ? (
            <>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2">
                <rect x="3" y="11" width="18" height="11" rx="2" />
                <path d="M7 11V7a5 5 0 0 1 10 0v4" />
              </svg>
              {t("payment.payCta", { total })}
            </>
          ) : (
            t("payment.placeOrderCta")
          )}
        </button>
      </div>
    </div>
  );
}
