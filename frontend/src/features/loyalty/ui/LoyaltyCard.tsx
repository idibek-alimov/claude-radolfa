"use client";

import { Star, Percent, BadgeDollarSign } from "lucide-react";
import { useTranslations } from "next-intl";
import type { LoyaltyProfile } from "@/entities/loyalty";
import { formatPrice } from "@/shared/lib";

interface LoyaltyCardProps {
  loyalty: LoyaltyProfile;
}

export default function LoyaltyCard({ loyalty }: LoyaltyCardProps) {
  const t = useTranslations("profile");
  const { points, tier } = loyalty;

  // Premium metallic gradient — diagonal sweep with highlight
  const cardBackground = tier
    ? `linear-gradient(135deg, ${tier.color} 0%, ${tier.color}dd 40%, ${tier.color}bb 60%, ${tier.color}ee 100%)`
    : "linear-gradient(135deg, #6366F1 0%, #8B5CF6 50%, #7C3AED 100%)";

  return (
    <div
      className="relative rounded-2xl p-6 text-white overflow-hidden shadow-lg"
      style={{ background: cardBackground }}
    >
      {/* Metallic sheen overlay */}
      <div
        className="absolute inset-0 pointer-events-none"
        style={{
          background:
            "linear-gradient(105deg, transparent 30%, rgba(255,255,255,0.12) 45%, rgba(255,255,255,0.06) 55%, transparent 70%)",
        }}
      />

      {/* Content */}
      <div className="relative">
        {/* Top row: points + tier name */}
        <div className="flex items-start justify-between">
          <div>
            <p className="text-sm font-medium text-white/80">{t("availablePoints")}</p>
            <p className="text-4xl font-bold mt-1 tracking-tight">{points}</p>
          </div>
          <div className="flex flex-col items-end gap-1.5">
            <Star className="h-10 w-10 text-white/20" />
            {tier && (
              <span className="text-sm font-semibold bg-white/15 backdrop-blur-sm px-3 py-1 rounded-full border border-white/10">
                {tier.name}
              </span>
            )}
          </div>
        </div>

        {/* Bottom row: tier benefits */}
        {tier ? (
          <div className="flex gap-6 mt-5 pt-4 border-t border-white/15">
            <div className="flex items-center gap-2.5">
              <div className="h-8 w-8 rounded-lg bg-white/15 flex items-center justify-center">
                <Percent className="h-4 w-4" />
              </div>
              <div>
                <p className="text-xl font-bold leading-none">{tier.discountPercentage}%</p>
                <p className="text-[11px] text-white/70 mt-0.5">{t("discount")}</p>
              </div>
            </div>
            <div className="flex items-center gap-2.5">
              <div className="h-8 w-8 rounded-lg bg-white/15 flex items-center justify-center">
                <BadgeDollarSign className="h-4 w-4" />
              </div>
              <div>
                <p className="text-xl font-bold leading-none">{tier.cashbackPercentage}%</p>
                <p className="text-[11px] text-white/70 mt-0.5">{t("cashback")}</p>
              </div>
            </div>
          </div>
        ) : (
          <div className="mt-4">
            <p className="text-sm font-semibold text-white/90">{t("startEarning")}</p>
            {loyalty.spendToNextTier != null ? (
              <p className="text-sm text-white/70 mt-1">
                {t("spendToNextTier", { amount: formatPrice(loyalty.spendToNextTier) })}
              </p>
            ) : (
              <p className="text-sm text-white/70 mt-1">{t("earnPoints")}</p>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
