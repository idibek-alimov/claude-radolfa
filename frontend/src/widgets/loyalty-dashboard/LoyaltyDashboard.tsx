"use client";

import { Star } from "lucide-react";
import { useTranslations } from "next-intl";
import type { LoyaltyProfile } from "@/entities/loyalty";
import { LoyaltyCard, TierProgress, TiersList } from "@/features/loyalty";

interface LoyaltyDashboardProps {
  loyalty: LoyaltyProfile;
}

export default function LoyaltyDashboard({ loyalty }: LoyaltyDashboardProps) {
  const t = useTranslations("profile");

  return (
    <div className="bg-card rounded-xl border shadow-sm p-6 sm:p-8">
      <h2 className="text-lg font-semibold text-foreground mb-6">
        {t("loyaltyPoints")}
      </h2>

      <div className="space-y-6">
        <LoyaltyCard loyalty={loyalty} />
        <TierProgress loyalty={loyalty} />
        <TiersList currentTierName={loyalty.tier?.name ?? null} />

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
