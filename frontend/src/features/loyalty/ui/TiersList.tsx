"use client";

import { useState } from "react";
import { Star, Check, ChevronDown, ChevronUp } from "lucide-react";
import { useTranslations } from "next-intl";
import { useLoyaltyTiers } from "@/entities/loyalty";
import { Skeleton } from "@/shared/ui/skeleton";
import { formatPrice } from "@/shared/lib/format";
import type { LoyaltyTier } from "@/entities/loyalty";

interface TiersListProps {
  currentTierName: string | null;
}

function TierCard({
  tier,
  label,
  isCurrent,
  t,
}: {
  tier: LoyaltyTier;
  label: string | null;
  isCurrent: boolean;
  t: ReturnType<typeof useTranslations<"profile">>;
}) {
  return (
    <div
      className={`rounded-xl border-2 p-4 transition-colors ${
        !isCurrent ? "border-border bg-card" : ""
      }`}
      style={isCurrent ? { borderColor: tier.color, backgroundColor: `${tier.color}0d` } : undefined}
    >
      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center gap-2">
          <div
            className={`h-7 w-7 rounded-full flex items-center justify-center ${
              !isCurrent ? "opacity-50" : ""
            }`}
            style={{ backgroundColor: tier.color, color: "white" }}
          >
            <Star
              className={`h-3.5 w-3.5 ${isCurrent ? "fill-current" : ""}`}
            />
          </div>
          <span className="text-sm font-semibold">{tier.name}</span>
          {label && (
            <span
              className="flex items-center gap-0.5 text-[10px] font-medium px-1.5 py-0.5 rounded-full"
              style={
                isCurrent
                  ? { color: tier.color, backgroundColor: `${tier.color}1a` }
                  : { color: "var(--muted-foreground)", backgroundColor: "var(--muted)" }
              }
            >
              {isCurrent && <Check className="h-2.5 w-2.5" />}
              {label}
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
}

export default function TiersList({ currentTierName }: TiersListProps) {
  const t = useTranslations("profile");
  const { data: tiers, isLoading } = useLoyaltyTiers();
  const [showAll, setShowAll] = useState(false);

  if (isLoading) {
    return (
      <div className="space-y-3">
        {Array.from({ length: 2 }).map((_, i) => (
          <Skeleton key={i} className="h-20 w-full rounded-xl" />
        ))}
      </div>
    );
  }

  if (!tiers || tiers.length === 0) return null;

  // Tiers come sorted by displayOrder ASC (best tier first)
  const currentIdx = currentTierName
    ? tiers.findIndex((t) => t.name === currentTierName)
    : -1;

  // Build the contextual slice
  let contextTiers: { tier: LoyaltyTier; label: string | null; isCurrent: boolean }[];

  if (currentIdx === -1) {
    // No tier — show only the entry-level tier (last in list = lowest rank)
    const entryTier = tiers[tiers.length - 1];
    contextTiers = [{ tier: entryTier, label: t("goalTier"), isCurrent: false }];
  } else if (currentIdx === 0) {
    // At top tier — show current only
    contextTiers = [{ tier: tiers[0], label: t("yourTier"), isCurrent: true }];
  } else {
    // Has tier + not at top — show current + next
    contextTiers = [
      { tier: tiers[currentIdx], label: t("yourTier"), isCurrent: true },
      { tier: tiers[currentIdx - 1], label: t("nextTier"), isCurrent: false },
    ];
  }

  const hasMore = tiers.length > contextTiers.length;

  return (
    <div className="space-y-3">
      {/* Contextual tiers */}
      {contextTiers.map(({ tier, label, isCurrent }) => (
        <TierCard key={tier.name} tier={tier} label={label} isCurrent={isCurrent} t={t} />
      ))}

      {/* Expanded: remaining tiers */}
      {showAll &&
        tiers
          .filter((tier) => !contextTiers.some((ct) => ct.tier.name === tier.name))
          .map((tier) => (
            <TierCard
              key={tier.name}
              tier={tier}
              label={null}
              isCurrent={false}
              t={t}
            />
          ))}

      {/* Toggle */}
      {hasMore && (
        <button
          onClick={() => setShowAll((v) => !v)}
          className="flex items-center gap-1 text-xs font-medium text-muted-foreground hover:text-foreground transition-colors mx-auto"
        >
          {showAll ? (
            <>
              {t("hideTiers")}
              <ChevronUp className="h-3.5 w-3.5" />
            </>
          ) : (
            <>
              {t("seeAllTiers")}
              <ChevronDown className="h-3.5 w-3.5" />
            </>
          )}
        </button>
      )}
    </div>
  );
}
