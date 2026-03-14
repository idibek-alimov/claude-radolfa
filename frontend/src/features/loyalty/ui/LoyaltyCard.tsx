"use client";

import { Star, Percent, BadgeDollarSign } from "lucide-react";
import { useTranslations } from "next-intl";
import type { LoyaltyProfile } from "@/entities/loyalty";

interface LoyaltyCardProps {
  loyalty: LoyaltyProfile;
}

export default function LoyaltyCard({ loyalty }: LoyaltyCardProps) {
  const t = useTranslations("profile");
  const { points, tier } = loyalty;

  return (
    <div
      className="rounded-xl p-6 text-white"
      style={{
        background: tier
          ? `linear-gradient(135deg, ${tier.color}, ${tier.color}cc, ${tier.color}99)`
          : "linear-gradient(135deg, #6366F1, #8B5CF6, #7C3AED)",
      }}
    >
      {/* Top row: points + tier name */}
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm opacity-90">{t("availablePoints")}</p>
          <p className="text-4xl font-bold mt-1">{points}</p>
        </div>
        <div className="flex flex-col items-end gap-1">
          <Star className="h-10 w-10 opacity-30" />
          {tier && (
            <span className="text-sm font-semibold bg-white/20 px-2.5 py-0.5 rounded-full">
              {tier.name}
            </span>
          )}
        </div>
      </div>

      {/* Bottom row: tier benefits */}
      {tier ? (
        <div className="flex gap-4 mt-4 pt-4 border-t border-white/20">
          <div className="flex items-center gap-2">
            <Percent className="h-4 w-4 opacity-75" />
            <div>
              <p className="text-lg font-bold leading-none">{tier.discountPercentage}%</p>
              <p className="text-[11px] opacity-75">{t("discount")}</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <BadgeDollarSign className="h-4 w-4 opacity-75" />
            <div>
              <p className="text-lg font-bold leading-none">{tier.cashbackPercentage}%</p>
              <p className="text-[11px] opacity-75">{t("cashback")}</p>
            </div>
          </div>
        </div>
      ) : (
        <p className="text-xs opacity-75 mt-3">{t("earnPoints")}</p>
      )}
    </div>
  );
}
