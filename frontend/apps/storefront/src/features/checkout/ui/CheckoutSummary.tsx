"use client";

import Image from "next/image";
import { useTranslations } from "next-intl";
import { Crown, ShieldCheck, BadgeCheck } from "lucide-react";
import { useAuth } from "@radolfa/shared/auth";
import { formatPrice } from "@radolfa/shared/lib/format";
import type { useCheckout } from "../hooks/useCheckout";
import { PointsRedeem } from "./PointsRedeem";

interface CheckoutSummaryProps {
  checkout: ReturnType<typeof useCheckout>;
}

export function CheckoutSummary({ checkout }: CheckoutSummaryProps) {
  const t = useTranslations("checkout");
  const tc = useTranslations("cart");
  const { user } = useAuth();

  const { cart, paymentMethod, codHandlingFee, pointsToRedeem, setPointsToRedeem } = checkout;

  if (!cart) return null;

  const availablePoints = user?.loyalty?.points ?? 0;
  const pointsValue = pointsToRedeem * 0.01;
  const codFee = paymentMethod === "COD" ? codHandlingFee : 0;
  const estimatedTotal = Math.max(0, cart.totalAmount - pointsValue + codFee);
  const crownTierPercent =
    cart.items.find((i) => i.mechanism === "LOYALTY")?.discountPercent ?? null;

  return (
    <div className="flex flex-col gap-4">
      <div className="bg-white rounded-2xl border border-ink/8 p-5">
        <div className="font-black text-[17px] mb-4">{tc("orderSummary")}</div>

        <div className="flex flex-col gap-3 max-h-52 overflow-y-auto scrollbar-hide pr-1">
          {cart.items.map((item) => (
            <div key={item.skuId} className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-lg overflow-hidden bg-plum/30 shrink-0 relative">
                {item.imageUrl ? (
                  <Image
                    src={item.imageUrl}
                    alt={item.productName}
                    fill
                    className="object-cover"
                    unoptimized
                  />
                ) : (
                  <div className="w-full h-full flex items-center justify-center text-ink/40 text-[8px]">
                    {tc("noImage")}
                  </div>
                )}
                <span className="absolute -top-1 -right-1 w-5 h-5 rounded-full bg-ink text-white text-[10px] font-bold flex items-center justify-center">
                  {item.quantity}
                </span>
              </div>
              <div className="flex-1 min-w-0">
                <div className="text-[12px] font-semibold truncate">{item.productName}</div>
                {item.category && (
                  <div className="text-[11px] text-ink/50">{item.category}</div>
                )}
              </div>
              <div className="text-[12px] font-bold tabular-nums">
                {formatPrice(item.lineTotal)}
              </div>
            </div>
          ))}
        </div>

        {cart.couponCode && (
          <div className="mt-4 flex items-center gap-2 rounded-full bg-emerald/10 text-emerald px-3 py-2 text-[12px] font-semibold">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M9 12l2 2 4-4" />
              <circle cx="12" cy="12" r="9" />
            </svg>
            {t("summary.couponApplied", { code: cart.couponCode })}
          </div>
        )}

        <div className="border-t border-ink/8 mt-4 pt-4 flex flex-col gap-2.5 text-[13px]">
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
        </div>

        <div className="border-t border-ink/8 mt-4 pt-4 flex items-baseline justify-between">
          <span className="font-bold text-[15px]">{tc("total")}</span>
          <div className="text-right">
            <div className="font-black text-[23px] tabular-nums leading-none">
              {formatPrice(estimatedTotal)}
            </div>
            {cart.savings > 0 && (
              <div className="text-[11px] text-emerald font-semibold mt-1">
                {tc("youSave", { amount: formatPrice(cart.savings) })}
              </div>
            )}
          </div>
        </div>

        <PointsRedeem
          availablePoints={availablePoints}
          pointsToRedeem={pointsToRedeem}
          setPointsToRedeem={setPointsToRedeem}
        />
      </div>

      <div className="bg-white rounded-2xl border border-ink/8 p-4 flex flex-col gap-3 text-[12px]">
        <div className="flex items-center gap-2.5">
          <ShieldCheck className="h-[17px] w-[17px] shrink-0" stroke="#1F8A5B" strokeWidth={2} />
          <span>
            <span className="font-semibold">{t("summary.encrypted")}</span> · {t("summary.encryptedSub")}
          </span>
        </div>
        <div className="flex items-center gap-2.5">
          <BadgeCheck className="h-[17px] w-[17px] shrink-0" stroke="#1F8A5B" strokeWidth={2} />
          <span>
            <span className="font-semibold">{t("summary.qualityGuarantee")}</span> ·{" "}
            {t("summary.qualityGuaranteeSub")}
          </span>
        </div>
      </div>
    </div>
  );
}
