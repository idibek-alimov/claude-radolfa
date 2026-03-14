"use client";

import { Star, Check } from "lucide-react";
import { useTranslations } from "next-intl";
import { useLoyaltyTiers } from "@/entities/loyalty";
import { Skeleton } from "@/shared/ui/skeleton";
import { formatPrice } from "@/shared/lib/format";

interface TiersListProps {
  currentTierName: string | null;
}

export default function TiersList({ currentTierName }: TiersListProps) {
  const t = useTranslations("profile");
  const { data: tiers, isLoading } = useLoyaltyTiers();

  if (isLoading) {
    return (
      <div className="space-y-3">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-20 w-full rounded-xl" />
        ))}
      </div>
    );
  }

  if (!tiers || tiers.length === 0) return null;

  return (
    <div className="space-y-3">
      <h3 className="text-sm font-semibold text-foreground">
        {t("allTiers")}
      </h3>
      {tiers.map((tier) => {
        const isCurrent = tier.name === currentTierName;
        return (
          <div
            key={tier.name}
            className={`rounded-xl border p-4 transition-colors ${
              isCurrent
                ? "border-primary bg-primary/5"
                : "border-border bg-card"
            }`}
          >
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-2">
                <div
                  className={`h-7 w-7 rounded-full flex items-center justify-center ${
                    isCurrent
                      ? "bg-primary text-primary-foreground"
                      : "bg-muted text-muted-foreground"
                  }`}
                >
                  <Star
                    className={`h-3.5 w-3.5 ${isCurrent ? "fill-current" : ""}`}
                  />
                </div>
                <span className="text-sm font-semibold">{tier.name}</span>
                {isCurrent && (
                  <span className="flex items-center gap-0.5 text-[10px] font-medium text-primary bg-primary/10 px-1.5 py-0.5 rounded-full">
                    <Check className="h-2.5 w-2.5" />
                    {t("yourTier")}
                  </span>
                )}
              </div>
            </div>
            <div className="grid grid-cols-3 gap-2 text-xs text-muted-foreground">
              <div>
                <p className="font-medium text-foreground">
                  {tier.discountPercentage}%
                </p>
                <p>{t("discount")}</p>
              </div>
              <div>
                <p className="font-medium text-foreground">
                  {tier.cashbackPercentage}%
                </p>
                <p>{t("cashback")}</p>
              </div>
              <div>
                <p className="font-medium text-foreground">
                  {formatPrice(tier.minSpendRequirement)}
                </p>
                <p>{t("minSpend")}</p>
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}
