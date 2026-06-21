"use client";

import { useState } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { Truck, Store } from "lucide-react";
import { cn } from "@radolfa/shared/lib/utils";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@radolfa/shared/ui/select";
import { useActivePickpoints } from "@/entities/pickpoint";
import { TIME_WINDOW_CODES, type TimeWindowCode } from "../model/types";
import type { useCheckout } from "../hooks/useCheckout";
import { FauxMap } from "./FauxMap";

interface DeliveryStepProps {
  checkout: ReturnType<typeof useCheckout>;
}

export function DeliveryStep({ checkout }: DeliveryStepProps) {
  const t = useTranslations("checkout");
  const { data: pickpoints } = useActivePickpoints();
  const [submitted, setSubmitted] = useState(false);

  const {
    deliveryType,
    setDeliveryType,
    address,
    setAddress,
    timeWindow,
    setTimeWindow,
    notes,
    setNotes,
    addressMissing,
    pickpointMissing,
    deliveryInvalid,
    next,
  } = checkout;

  function handleContinue() {
    setSubmitted(true);
    if (!deliveryInvalid) next();
  }

  return (
    <div className="flex flex-col gap-5">
      <div className="bg-white rounded-2xl border border-ink/8 p-4 md:p-6">
        <h2 className="font-black text-[15px] md:text-[19px] mb-3 md:mb-4">
          {t("delivery.title")}
        </h2>

        {/* Method segments */}
        <div className="grid grid-cols-2 gap-2.5 md:gap-3">
          <label
            className={cn(
              "rounded-xl border-2 p-3 md:p-4 flex flex-col gap-1.5 md:gap-2 relative cursor-pointer",
              deliveryType === "HOME"
                ? "border-mag bg-mag/5"
                : "border-ink/10 hover:border-ink/25"
            )}
          >
            <input
              type="radio"
              name="deliveryType"
              checked={deliveryType === "HOME"}
              onChange={() => setDeliveryType("HOME")}
              className="absolute top-2.5 md:top-3 right-2.5 md:right-3 w-4 h-4 accent-mag"
            />
            <Truck
              className="h-5 w-5 md:h-[22px] md:w-[22px]"
              color={deliveryType === "HOME" ? "#CB11AB" : "currentColor"}
            />
            <div className="text-[13px] md:text-[14px] font-bold">{t("delivery.home.title")}</div>
            <div className="text-[11px] md:text-[12px] text-ink/55 leading-snug">
              {t("delivery.home.description")}
            </div>
            <div className="text-[11px] md:text-[12px] font-bold text-emerald">
              {t("delivery.home.free")}
            </div>
          </label>

          <label
            className={cn(
              "rounded-xl border-2 p-3 md:p-4 flex flex-col gap-1.5 md:gap-2 relative cursor-pointer",
              deliveryType === "PICKPOINT"
                ? "border-mag bg-mag/5"
                : "border-ink/10 hover:border-ink/25"
            )}
          >
            <input
              type="radio"
              name="deliveryType"
              checked={deliveryType === "PICKPOINT"}
              onChange={() => setDeliveryType("PICKPOINT")}
              className="absolute top-2.5 md:top-3 right-2.5 md:right-3 w-4 h-4 accent-mag"
            />
            <Store
              className="h-5 w-5 md:h-[22px] md:w-[22px]"
              color={deliveryType === "PICKPOINT" ? "#CB11AB" : "currentColor"}
            />
            <div className="text-[13px] md:text-[14px] font-bold">{t("delivery.pickpoint.title")}</div>
            <div className="text-[11px] md:text-[12px] text-ink/55 leading-snug">
              {t("delivery.pickpoint.description")}
            </div>
            <div className="text-[11px] md:text-[12px] font-bold text-emerald">
              {t("delivery.pickpoint.free")}
            </div>
          </label>
        </div>

        {/* Courier panel */}
        {deliveryType === "HOME" && (
          <div className="mt-5 pt-5 border-t border-ink/8">
            <h3 className="text-[14px] font-bold mb-3">{t("delivery.home.addressLabel")}</h3>

            <div className="relative mb-3">
              <svg
                width="17"
                height="17"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                className="absolute left-3.5 top-1/2 -translate-y-1/2 text-ink/35 pointer-events-none"
              >
                <circle cx="11" cy="11" r="7" />
                <path d="m21 21-4.3-4.3" />
              </svg>
              <input
                className="field pl-10"
                value={address}
                onChange={(e) => setAddress(e.target.value)}
                placeholder={t("delivery.home.addressPlaceholder")}
              />
            </div>
            {submitted && addressMissing && (
              <p className="text-xs text-destructive mb-3">{t("delivery.home.addressRequired")}</p>
            )}

            <FauxMap />

            <div className="mb-3">
              <label className="text-[12px] font-semibold text-ink/60 mb-1.5 block">
                {t("delivery.home.timeWindowLabel")}
              </label>
              <Select
                value={timeWindow}
                onValueChange={(v) => setTimeWindow(v as TimeWindowCode)}
              >
                <SelectTrigger className="h-[46px] rounded-xl border border-ink/10 bg-white px-3.5 text-[14px]">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {TIME_WINDOW_CODES.map((code) => (
                    <SelectItem key={code} value={code}>
                      {t(`delivery.timeWindow.${code}` as Parameters<typeof t>[0])}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
        )}

        {/* Pickpoint panel (placeholder — real list in Phase 9) */}
        {deliveryType === "PICKPOINT" && (
          <div className="mt-5 pt-5 border-t border-ink/8">
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-[14px] font-bold">{t("delivery.pickpoint.selectLabel")}</h3>
              <span className="text-[12px] text-ink/45 font-medium">
                {t("delivery.pickpoint.nearYou", { count: pickpoints?.length ?? 0 })}
              </span>
            </div>
            <p className="text-[13px] text-ink/50">{t("delivery.pickpoint.comingSoon")}</p>
            {submitted && pickpointMissing && (
              <p className="text-xs text-destructive mt-2">{t("delivery.pickpoint.required")}</p>
            )}
          </div>
        )}

        {/* Delivery note */}
        <div className="mt-5 pt-5 border-t border-ink/8">
          <label className="text-[14px] font-bold block">
            {t("notes")}{" "}
            <span className="text-[12px] font-normal text-ink/40">— {t("notesOptional")}</span>
          </label>
          <p className="text-[12px] text-ink/50 mt-1 mb-2.5">{t("notesHint")}</p>
          <textarea
            className="field-area"
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder={t("notesPlaceholder")}
          />
        </div>
      </div>

      {/* Footer */}
      <div className="flex items-center justify-between">
        <Link
          href="/cart"
          className="text-[14px] font-semibold text-mag hover:text-maglo inline-flex items-center gap-1.5"
        >
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2">
            <path d="M19 12H5" />
            <path d="m12 19-7-7 7-7" />
          </svg>
          {t("delivery.returnToBag")}
        </Link>
        <button
          onClick={handleContinue}
          disabled={deliveryInvalid}
          className="h-12 px-8 rounded-full bg-mag text-white font-bold text-[15px] hover:bg-maglo transition inline-flex items-center gap-2 shadow-lg shadow-mag/20 disabled:opacity-50"
        >
          {t("delivery.continueToReview")}
          <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2">
            <path d="M5 12h14" />
            <path d="m12 5 7 7-7 7" />
          </svg>
        </button>
      </div>
    </div>
  );
}
