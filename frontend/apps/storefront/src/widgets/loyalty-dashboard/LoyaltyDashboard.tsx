"use client";

import { Star } from "lucide-react";
import { useTranslations } from "next-intl";
import type { LoyaltyProfile } from "@/entities/loyalty";
import { LoyaltyCard, TierProgress, TiersList } from "@/features/loyalty";
import { formatPrice } from "@/shared/lib/format";

interface LoyaltyDashboardProps {
  loyalty: LoyaltyProfile;
}

export default function LoyaltyDashboard({ loyalty }: LoyaltyDashboardProps) {
  const t = useTranslations("profile");

  const recentEarnings = loyalty.recentEarnings
    .slice()
    .sort((a, b) => b.orderedAt.localeCompare(a.orderedAt))
    .slice(0, 5);

  return (
    <div className="bg-card rounded-xl border shadow-sm p-6 sm:p-8">
      <h2 className="text-lg font-semibold text-foreground mb-6">
        {t("loyaltyPoints")}
      </h2>

      <div className="space-y-6">
        <LoyaltyCard loyalty={loyalty} />
        <TierProgress loyalty={loyalty} />
        <TiersList currentTierName={loyalty.tier?.name ?? null} />

        {/* Recent Earnings */}
        {recentEarnings.length > 0 && (
          <div className="rounded-xl border bg-accent/20 p-4">
            <p className="text-sm font-medium text-foreground mb-3">
              {t("recentEarnings")}
            </p>
            <ul className="space-y-2">
              {recentEarnings.map((entry) => (
                <li key={entry.orderId} className="flex items-center justify-between text-sm">
                  <div className="flex flex-col min-w-0">
                    <span className="text-muted-foreground text-xs">
                      {new Date(entry.orderedAt).toLocaleDateString()}
                    </span>
                    <span className="text-foreground truncate">
                      {formatPrice(entry.orderAmount)}
                    </span>
                  </div>
                  <span className="text-amber-600 font-semibold shrink-0 ml-3">
                    +{entry.pointsEarned} pts
                  </span>
                </li>
              ))}
            </ul>
          </div>
        )}

        {/* How it works */}
        <div className="rounded-xl border bg-accent/20 p-4">
          <p className="text-sm font-medium text-foreground mb-2">
            {t("loyaltyHowTitle")}
          </p>
          <ul className="space-y-1.5 text-sm text-muted-foreground">
            <li className="flex items-start gap-2">
              <Star className="h-4 w-4 text-amber-500 shrink-0 mt-0.5" />
              {t("loyaltyHowEarn")}
            </li>
            <li className="flex items-start gap-2">
              <Star className="h-4 w-4 text-amber-500 shrink-0 mt-0.5" />
              {t("loyaltyHowRedeem")}
            </li>
          </ul>
        </div>
      </div>
    </div>
  );
}
