"use client";

import { Star, AlertTriangle } from "lucide-react";
import { useTranslations } from "next-intl";
import type { LoyaltyProfile } from "@/entities/loyalty";

interface TierProgressProps {
  loyalty: LoyaltyProfile;
}

function getDaysRemainingInMonth(): number {
  const now = new Date();
  const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate();
  return lastDay - now.getDate();
}

export default function TierProgress({ loyalty }: TierProgressProps) {
  const t = useTranslations("profile");
  const { tier, spendToNextTier, spendToMaintainTier, currentMonthSpending } =
    loyalty;

  const spending = currentMonthSpending ?? 0;
  const nextTarget =
    spendToNextTier != null ? spending + spendToNextTier : null;
  const progress = nextTarget ? (spending / nextTarget) * 100 : tier ? 100 : 0;

  // Demotion warning
  const needsMaintenance =
    spendToMaintainTier != null && spendToMaintainTier > 0;
  const daysLeft = getDaysRemainingInMonth();
  const demotionSeverity: "info" | "warning" | "urgent" = needsMaintenance
    ? daysLeft <= 2
      ? "urgent"
      : daysLeft <= 7
        ? "warning"
        : "info"
    : "info";

  return (
    <div className="bg-accent/30 rounded-xl p-5 space-y-3">
      {/* Current tier badge */}
      {tier && (
        <div className="flex items-center gap-2">
          <div className="h-8 w-8 rounded-full bg-primary text-primary-foreground flex items-center justify-center">
            <Star className="h-3.5 w-3.5 fill-current" />
          </div>
          <span className="text-sm font-medium">{tier.name}</span>
        </div>
      )}

      {/* Progress bar */}
      <div className="h-2 bg-muted rounded-full overflow-hidden">
        <div
          className="h-full bg-gradient-to-r from-primary to-purple-500 rounded-full transition-all duration-500"
          style={{ width: `${Math.min(progress, 100)}%` }}
        />
      </div>

      {/* Status text */}
      <p className="text-xs text-muted-foreground text-center">
        {spendToNextTier != null
          ? t("spendToNextTier", { amount: spendToNextTier.toFixed(0) })
          : tier
            ? t("topTier")
            : t("startEarning")}
      </p>

      {/* Demotion warning */}
      {needsMaintenance && (
        <div
          className={`flex items-center gap-2 rounded-lg px-3 py-2 text-xs font-medium ${
            demotionSeverity === "urgent"
              ? "bg-red-100 text-red-800"
              : demotionSeverity === "warning"
                ? "bg-yellow-100 text-yellow-800"
                : "bg-blue-50 text-blue-700"
          }`}
        >
          <AlertTriangle className="h-3.5 w-3.5 shrink-0" />
          {t("demotionWarning", {
            amount: spendToMaintainTier!.toFixed(0),
            days: daysLeft,
          })}
        </div>
      )}
    </div>
  );
}
