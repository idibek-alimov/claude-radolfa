"use client";

import { useState } from "react";
import Link from "next/link";
import { X, Crown, ShieldCheck, Trash2, Loader2 } from "lucide-react";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { getErrorMessage, isCouponsEnabled } from "@radolfa/shared/lib";
import { useApplyCoupon, useRemoveCoupon, useClearCart } from "@/features/cart";
import { formatPrice } from "@radolfa/shared/lib/format";
import type { ApplyCouponResponse, Cart } from "@/entities/cart";

interface OrderSummaryProps {
  cart: Cart;
  hasOutOfStock: boolean;
  /** discountPercent of any LOYALTY-mechanism line — same for the whole user, null when no such line. */
  crownTierPercent: number | null;
}

export function OrderSummary({ cart, hasOutOfStock, crownTierPercent }: OrderSummaryProps) {
  const t = useTranslations("cart");
  const tc = useTranslations("checkout");
  const [couponInput, setCouponInput] = useState("");
  const [couponError, setCouponError] = useState<string | null>(null);
  const [affectedCount, setAffectedCount] = useState<number | null>(null);

  const applyCouponMutation = useApplyCoupon();
  const removeCouponMutation = useRemoveCoupon();
  const clearCart = useClearCart();

  function applyCoupon() {
    const code = couponInput.trim();
    if (!code) return;
    setCouponError(null);
    applyCouponMutation.mutate(code, {
      onSuccess: (data: ApplyCouponResponse) => {
        if (data.valid) {
          setCouponInput("");
          setAffectedCount(data.affectedSkus.length);
        } else {
          const key = `coupon.${data.invalidReason}` as Parameters<typeof tc>[0];
          setCouponError(tc(key));
        }
      },
      onError: (err: unknown) => setCouponError(getErrorMessage(err)),
    });
  }

  return (
    <div className="bg-white rounded-2xl border border-ink/8 p-5">
      <div className="font-black text-[18px]">{t("orderSummary")}</div>

      {isCouponsEnabled && (
        <div className="mt-4">
          {cart.couponCode ? (
            <div className="space-y-2">
              <div className="flex items-center gap-2">
                <span className="inline-flex items-center gap-1.5 rounded-full border border-mag/30 bg-mag/10 text-mag text-[13px] font-mono font-medium px-3 py-1.5">
                  {cart.couponCode}
                </span>
                <button
                  className="h-9 w-9 flex items-center justify-center rounded-full text-ink/55 hover:text-sale disabled:opacity-40"
                  onClick={() => {
                    setAffectedCount(null);
                    removeCouponMutation.mutate(undefined, {
                      onError: (err) => toast.error(getErrorMessage(err)),
                    });
                  }}
                  disabled={removeCouponMutation.isPending}
                >
                  <X className="h-4 w-4" />
                </button>
              </div>
              {affectedCount !== null && (
                <p className="text-[12px] text-ink/55">
                  {tc("coupon.affectedItems", { count: affectedCount })}
                </p>
              )}
            </div>
          ) : (
            <div className="flex gap-2">
              <input
                className="flex-1 h-11 px-4 rounded-full bg-plum/40 text-[13px] focus:outline-none focus:bg-white focus:ring-2 focus:ring-mag border border-transparent focus:border-mag/40"
                placeholder={tc("coupon.placeholder")}
                value={couponInput}
                maxLength={32}
                onChange={(e) => {
                  setCouponInput(e.target.value.toUpperCase());
                  setCouponError(null);
                }}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    applyCoupon();
                  }
                }}
              />
              <button
                className="h-11 px-5 rounded-full border border-mag text-mag text-[13px] font-bold hover:bg-mag hover:text-white transition disabled:opacity-50"
                disabled={!couponInput.trim() || applyCouponMutation.isPending}
                onClick={applyCoupon}
              >
                {applyCouponMutation.isPending ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  tc("coupon.apply")
                )}
              </button>
            </div>
          )}
          {couponError && <p className="text-[12px] text-sale mt-2">{couponError}</p>}
        </div>
      )}

      <div className="mt-5 flex flex-col gap-2.5 text-[14px]">
        <div className="flex items-center justify-between">
          <span className="text-ink/65">{t("itemCount", { count: cart.itemCount })}</span>
          <span className="tabular-nums font-medium">{formatPrice(cart.subtotal)}</span>
        </div>
        {cart.itemDiscounts > 0 && (
          <div className="flex items-center justify-between text-emerald">
            <span>{t("itemDiscounts")}</span>
            <span className="tabular-nums font-medium">−{formatPrice(cart.itemDiscounts)}</span>
          </div>
        )}
        {cart.crownTier > 0 && crownTierPercent != null && (
          <div className="flex items-center justify-between text-[#9A6E0F]">
            <span className="inline-flex items-center gap-1.5">
              <Crown className="h-3 w-3" fill="#FFCC4F" stroke="none" />
              {t("crownTierRow", { percent: crownTierPercent })}
            </span>
            <span className="tabular-nums font-medium">−{formatPrice(cart.crownTier)}</span>
          </div>
        )}
        <div className="flex items-center justify-between">
          <span className="text-ink/65">{t("shipping")}</span>
          <span className="tabular-nums font-semibold text-emerald">{t("free")}</span>
        </div>
      </div>

      <div className="border-t border-ink/8 mt-4 pt-4 flex items-baseline justify-between">
        <span className="font-bold text-[15px]">{t("total")}</span>
        <div className="text-right">
          <div className="font-black text-[24px] tabular-nums leading-none">
            {formatPrice(cart.totalAmount)}
          </div>
          {cart.savings > 0 && (
            <div className="text-[11px] text-emerald font-semibold mt-1">
              {t("youSave", { amount: formatPrice(cart.savings) })}
            </div>
          )}
        </div>
      </div>

      <Link
        href="/checkout"
        aria-disabled={hasOutOfStock}
        className={`mt-4 w-full h-13 py-4 rounded-full bg-mag text-white font-bold text-[15px] transition inline-flex items-center justify-center gap-2 shadow-lg shadow-mag/20 ${hasOutOfStock ? "opacity-50 pointer-events-none" : "hover:bg-maglo"}`}
      >
        {t("proceedToCheckout")}
      </Link>

      <div className="mt-3 flex items-center justify-center gap-1.5 text-[12px] text-ink/55">
        <ShieldCheck className="h-[14px] w-[14px]" strokeWidth={2} />
        {t("secureCheckout")}
      </div>

      <button
        onClick={() => clearCart.mutate()}
        disabled={clearCart.isPending}
        className="w-full flex items-center justify-center gap-1.5 text-[12px] text-ink/45 hover:text-sale transition-colors py-1 mt-3"
      >
        <Trash2 className="h-3.5 w-3.5" />
        {t("clearCart")}
      </button>
    </div>
  );
}
