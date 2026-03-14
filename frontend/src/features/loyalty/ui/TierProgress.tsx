"use client";

import { Star, AlertTriangle } from "lucide-react";
import { useTranslations } from "next-intl";
import type { LoyaltyProfile } from "@/entities/loyalty";
import { useLoyaltyTiers } from "@/entities/loyalty";

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
  const { data: tiers } = useLoyaltyTiers();
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

  // Tiers come sorted by displayOrder ASC (best first) — reverse for ladder (lowest → highest)
  const ladderTiers = tiers ? [...tiers].reverse() : [];
  const currentIdx = tier
    ? ladderTiers.findIndex((lt) => lt.name === tier.name)
    : -1;

  // Determine the two tiers to display
  const currentTier = currentIdx >= 0 ? ladderTiers[currentIdx] : null;
  const nextTier =
    currentIdx >= 0 && currentIdx < ladderTiers.length - 1
      ? ladderTiers[currentIdx + 1]
      : currentIdx === -1 && ladderTiers.length > 0
        ? ladderTiers[0] // no tier yet — first tier is the goal
        : null;

  return (
    <div className="bg-accent/30 rounded-xl p-5 space-y-3">
      {/* Two-tier visual: current → next */}
      {(currentTier || nextTier) && (
        <div className="flex items-center gap-3">
          {/* Current tier (or empty state) */}
          <div className="flex flex-col items-center gap-1 shrink-0 w-16">
            <div
              className={`h-10 w-10 rounded-full flex items-center justify-center ${
                !currentTier ? "bg-muted text-muted-foreground" : ""
              }`}
              style={currentTier ? { backgroundColor: currentTier.color, color: "white" } : undefined}
            >
              <Star
                className={`h-4 w-4 ${currentTier ? "fill-current" : ""}`}
              />
            </div>
            <span className="text-[10px] sm:text-xs font-medium text-center leading-tight truncate max-w-[64px]">
              {currentTier?.name ?? t("startEarning")}
            </span>
          </div>

          {/* Progress bar between them */}
          <div className="flex-1 flex flex-col gap-1">
            <div className="h-2 bg-muted rounded-full overflow-hidden">
              <div
                className="h-full rounded-full transition-all duration-500"
                style={{
                  width: `${Math.min(progress, 100)}%`,
                  background: currentTier && nextTier
                    ? `linear-gradient(90deg, ${currentTier.color}, ${nextTier.color})`
                    : currentTier
                      ? currentTier.color
                      : undefined,
                }}
              />
            </div>
            <p className="text-[10px] text-muted-foreground text-center">
              {spendToNextTier != null
                ? t("spendToNextTier", { amount: spendToNextTier.toFixed(0) })
                : tier
                  ? t("topTier")
                  : t("startEarning")}
            </p>
          </div>

          {/* Next tier (or top badge) */}
          {nextTier ? (
            <div className="flex flex-col items-center gap-1 shrink-0 w-16">
              <div
                className="h-10 w-10 rounded-full flex items-center justify-center opacity-40"
                style={{ backgroundColor: nextTier.color, color: "white" }}
              >
                <Star className="h-4 w-4" />
              </div>
              <span className="text-[10px] sm:text-xs text-muted-foreground text-center leading-tight truncate max-w-[64px]">
                {nextTier.name}
              </span>
            </div>
          ) : tier ? (
            <div className="flex flex-col items-center gap-1 shrink-0 w-16">
              <div className="h-10 w-10 rounded-full flex items-center justify-center bg-primary text-primary-foreground">
                <Star className="h-4 w-4 fill-current" />
              </div>
              <span className="text-[10px] sm:text-xs font-medium text-center leading-tight">
                {t("topTier")}
              </span>
            </div>
          ) : null}
        </div>
      )}

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
