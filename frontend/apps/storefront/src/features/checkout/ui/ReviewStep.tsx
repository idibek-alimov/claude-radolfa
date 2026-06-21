"use client";

import Image from "next/image";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { Crown } from "lucide-react";
import { useAuth } from "@radolfa/shared/auth";
import { formatPrice } from "@radolfa/shared/lib/format";
import { useActivePickpoints } from "@/entities/pickpoint";
import type { useCheckout } from "../hooks/useCheckout";
import { PointsRedeem } from "./PointsRedeem";

interface ReviewStepProps {
  checkout: ReturnType<typeof useCheckout>;
}

export function ReviewStep({ checkout }: ReviewStepProps) {
  const t = useTranslations("checkout");
  const tc = useTranslations("cart");
  const { user } = useAuth();
  const { data: pickpoints } = useActivePickpoints();

  const {
    cart,
    deliveryType,
    address,
    pickpointId,
    notes,
    paymentMethod,
    codHandlingFee,
    pointsToRedeem,
    setPointsToRedeem,
    goStep,
    next,
    back,
  } = checkout;

  if (!cart) return null;

  const pickpoint = pickpoints?.find((pp) => pp.id === pickpointId);
  const crownTierPercent =
    cart.items.find((i) => i.mechanism === "LOYALTY")?.discountPercent ?? null;
  const availablePoints = user?.loyalty?.points ?? 0;
  const pointsValue = pointsToRedeem * 0.01;
  const codFee = paymentMethod === "COD" ? codHandlingFee : 0;

  return (
    <div className="flex flex-col gap-5">
      {/* Delivery summary */}
      <div className="bg-white rounded-2xl border border-ink/8 p-4 md:p-5 flex items-start gap-3">
        <svg
          width="18"
          height="18"
          viewBox="0 0 24 24"
          fill="none"
          stroke="#CB11AB"
          strokeWidth="2"
          className="mt-0.5 shrink-0"
        >
          <path d="M20 10c0 6-8 11-8 11s-8-5-8-11a8 8 0 0 1 16 0z" />
          <circle cx="12" cy="10" r="3" />
        </svg>
        <div className="flex-1 min-w-0">
          <div className="text-[11px] uppercase tracking-wider font-semibold text-ink/45">
            {t("review.deliveryTo")}
          </div>
          <div className="text-[13px] md:text-[14px] font-semibold mt-0.5 md:mt-1">
            {[user?.name, user?.phone].filter(Boolean).join(" · ")}
          </div>
          <div className="text-[12px] md:text-[13px] text-ink/65 mt-0.5">
            {deliveryType === "HOME" ? address : pickpoint && `${pickpoint.name} · ${pickpoint.address}`}
          </div>
          <div className="text-[12px] md:text-[13px] text-emerald font-semibold mt-0.5 md:mt-1">
            {deliveryType === "HOME" ? t("review.methodCourier") : t("review.methodPickpoint")}
          </div>
          {notes && (
            <div className="text-[11px] md:text-[12px] text-ink/50 mt-1 md:mt-1.5 flex items-start gap-1.5">
              <svg
                width="13"
                height="13"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                className="mt-0.5 shrink-0"
              >
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
              </svg>
              <span>&ldquo;{notes}&rdquo;</span>
            </div>
          )}
        </div>
        <button
          onClick={() => goStep("delivery")}
          className="text-[12px] md:text-[13px] font-semibold text-mag hover:text-maglo shrink-0"
        >
          {t("review.edit")}
        </button>
      </div>

      {/* Items */}
      <div className="bg-white rounded-2xl border border-ink/8 p-4 md:p-5">
        <div className="flex items-center justify-between mb-3 md:mb-4">
          <h2 className="font-black text-[15px] md:text-[17px]">
            {t("review.items", { count: cart.itemCount })}
          </h2>
          <Link href="/cart" className="text-[12px] md:text-[13px] font-semibold text-mag hover:text-maglo">
            {t("review.editBag")}
          </Link>
        </div>
        <div className="flex flex-col gap-3 md:gap-4">
          {cart.items.map((item) => (
            <div key={item.skuId} className="flex gap-3 items-center">
              <div className="w-14 h-14 md:w-16 md:h-16 rounded-lg overflow-hidden bg-plum/30 shrink-0 relative">
                {item.imageUrl ? (
                  <Image
                    src={item.imageUrl}
                    alt={item.productName}
                    fill
                    className="object-cover"
                    unoptimized
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center text-ink/40 text-[10px]">
                    {tc("noImage")}
                  </div>
                )}
              </div>
              <div className="flex-1 min-w-0">
                <div className="text-[12px] md:text-[13px] font-semibold truncate">
                  {item.productName}
                </div>
                <div className="text-[11px] md:text-[12px] text-ink/55">
                  {[item.colorName, item.sizeLabel, t("review.qty", { count: item.quantity })]
                    .filter(Boolean)
                    .join(" · ")}
                </div>
              </div>
              <div className="text-[13px] md:text-[14px] font-black text-mag tabular-nums">
                {formatPrice(item.lineTotal)}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Mobile pricing breakdown */}
      <div className="md:hidden bg-white rounded-2xl border border-ink/8 p-4 flex flex-col gap-2.5 text-[13px]">
        {cart.couponCode && (
          <div className="flex items-center gap-2 rounded-full bg-emerald/10 text-emerald px-3 py-2 text-[12px] font-semibold w-fit">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M9 12l2 2 4-4" />
              <circle cx="12" cy="12" r="9" />
            </svg>
            {tc("coupon.applied")}: {cart.couponCode}
          </div>
        )}
        <div className="flex items-center justify-between">
          <span className="text-ink/65">{tc("itemCount", { count: cart.itemCount })}</span>
          <span className="tabular-nums font-medium">{formatPrice(cart.subtotal)}</span>
        </div>
        {cart.itemDiscounts > 0 && (
          <div className="flex items-center justify-between text-emerald">
            <span>{tc("itemDiscounts")}</span>
            <span className="tabular-nums font-medium">−{formatPrice(cart.itemDiscounts)}</span>
          </div>
        )}
        {cart.crownTier > 0 && crownTierPercent != null && (
          <div className="flex items-center justify-between" style={{ color: "#9A6E0F" }}>
            <span className="inline-flex items-center gap-1.5">
              <Crown className="h-3 w-3" fill="#FFCC4F" stroke="none" />
              {tc("crownTierRow", { percent: crownTierPercent })}
            </span>
            <span className="tabular-nums font-medium">−{formatPrice(cart.crownTier)}</span>
          </div>
        )}
        {pointsToRedeem > 0 && (
          <div className="flex items-center justify-between text-emerald">
            <span>{t("pointsDiscount")}</span>
            <span className="tabular-nums font-medium">−{formatPrice(pointsValue)}</span>
          </div>
        )}
        {codFee > 0 && (
          <div className="flex items-center justify-between">
            <span className="text-ink/65">{t("summary.codHandling")}</span>
            <span className="tabular-nums font-medium">+{formatPrice(codFee)}</span>
          </div>
        )}
        <div className="flex items-center justify-between">
          <span className="text-ink/65">{tc("shipping")}</span>
          <span className="tabular-nums font-semibold text-emerald">{tc("free")}</span>
        </div>

        <PointsRedeem
          availablePoints={availablePoints}
          pointsToRedeem={pointsToRedeem}
          setPointsToRedeem={setPointsToRedeem}
        />
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
          {t("review.backToDelivery")}
        </button>
        <button
          onClick={next}
          className="h-12 px-8 rounded-full bg-mag text-white font-bold text-[15px] hover:bg-maglo transition inline-flex items-center gap-2 shadow-lg shadow-mag/20"
        >
          {t("review.continueToPayment")}
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2">
            <path d="M5 12h14" />
            <path d="m12 5 7 7-7 7" />
          </svg>
        </button>
      </div>
    </div>
  );
}
