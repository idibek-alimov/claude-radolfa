"use client";

import { Star } from "lucide-react";
import { useTranslations } from "next-intl";
import type { LoyaltyProfile } from "@/entities/loyalty";

interface LoyaltyCardProps {
  loyalty: LoyaltyProfile;
}

export default function LoyaltyCard({ loyalty }: LoyaltyCardProps) {
  const t = useTranslations("profile");
  const { points, tier } = loyalty;

  return (
    <div className="bg-gradient-to-br from-indigo-500 via-purple-500 to-purple-600 rounded-xl p-6 text-white">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm opacity-90">{t("availablePoints")}</p>
          <p className="text-4xl font-bold mt-1">{points}</p>
          {tier ? (
            <div className="mt-2 space-y-0.5">
              <p className="text-sm font-medium opacity-90">
                {tier.name} — {tier.discountPercentage}% {t("discount")}
              </p>
              <p className="text-xs opacity-75">
                {tier.cashbackPercentage}% {t("cashback")}
              </p>
            </div>
          ) : (
            <p className="text-xs opacity-75 mt-2">{t("earnPoints")}</p>
          )}
        </div>
        <Star className="h-14 w-14 opacity-30" />
      </div>
    </div>
  );
}
