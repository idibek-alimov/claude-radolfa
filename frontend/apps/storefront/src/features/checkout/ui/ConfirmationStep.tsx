"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import { useAuth } from "@radolfa/shared/auth";
import { formatPrice } from "@radolfa/shared/lib/format";

interface ConfirmationStepProps {
  orderId: number;
  total: number;
  paymentMethod: "CARD" | "COD";
  deliveryType: "HOME" | "PICKPOINT" | null;
}

export function ConfirmationStep({
  orderId,
  total,
  paymentMethod,
  deliveryType,
}: ConfirmationStepProps) {
  const t = useTranslations("checkout.confirmation");
  const { user } = useAuth();

  return (
    <div className="bg-white rounded-2xl border border-ink/8 p-6 md:p-10 text-center max-w-2xl mx-auto">
      <div className="w-16 h-16 rounded-full bg-emerald/12 flex items-center justify-center mx-auto">
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#1F8A5B" strokeWidth="2.5">
          <path d="M20 6 9 17l-5-5" />
        </svg>
      </div>

      <h2 className="font-black text-[20px] md:text-[26px] mt-5">{t("title")}</h2>

      <p className="text-[13px] md:text-[14px] text-ink/60 mt-2 max-w-md mx-auto">
        {t("thankYou", { name: user?.name ?? "" })}{" "}
        {user?.email && t("thankYouEmail", { email: user.email })}
      </p>

      <div className="mt-5 inline-flex items-center gap-2 px-4 py-2.5 rounded-full bg-plum/40">
        <span className="text-[12px] text-ink/55">{t("orderNumber")}</span>
        <span className="font-mono font-bold text-[13px] md:text-[14px] text-mag">
          #{orderId}
        </span>
      </div>

      <div className="mt-6 grid grid-cols-3 gap-2 md:gap-3 text-left max-w-lg mx-auto">
        <div className="rounded-xl bg-soft/60 border border-ink/8 p-3">
          <div className="text-[10px] md:text-[11px] text-ink/50 uppercase tracking-wider font-semibold">
            {t("arrivesLabel")}
          </div>
          <div className="text-[12px] md:text-[13px] font-bold mt-1">
            {deliveryType === "PICKPOINT" ? t("arrivesPickpoint") : t("arrivesCourier")}
          </div>
        </div>
        <div className="rounded-xl bg-soft/60 border border-ink/8 p-3">
          <div className="text-[10px] md:text-[11px] text-ink/50 uppercase tracking-wider font-semibold">
            {t("deliveryLabel")}
          </div>
          <div className="text-[12px] md:text-[13px] font-bold mt-1">
            {deliveryType === "PICKPOINT" ? t("deliveryPickpoint") : t("deliveryCourier")}
          </div>
        </div>
        <div className="rounded-xl bg-soft/60 border border-ink/8 p-3">
          <div className="text-[10px] md:text-[11px] text-ink/50 uppercase tracking-wider font-semibold">
            {paymentMethod === "COD" ? t("codDueLabel") : t("paidLabel")}
          </div>
          <div className="text-[12px] md:text-[13px] font-bold mt-1 tabular-nums">
            {formatPrice(total)}
          </div>
        </div>
      </div>

      <div className="flex flex-col sm:flex-row items-center justify-center gap-3 mt-7">
        <Link
          href="/profile/orders"
          className="h-12 px-7 rounded-full bg-mag text-white font-bold text-[14px] hover:bg-maglo transition inline-flex items-center gap-2"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M5 7h14l-1.5 12.5a2 2 0 0 1-2 1.5H8.5a2 2 0 0 1-2-1.5z" />
            <path d="M9 7V5a3 3 0 0 1 6 0v2" />
          </svg>
          {t("trackOrder")}
        </Link>
        <Link
          href="/"
          className="h-12 px-7 rounded-full border border-ink/15 font-bold text-[14px] hover:bg-ink/5 transition inline-flex items-center"
        >
          {t("continueShopping")}
        </Link>
      </div>
    </div>
  );
}
