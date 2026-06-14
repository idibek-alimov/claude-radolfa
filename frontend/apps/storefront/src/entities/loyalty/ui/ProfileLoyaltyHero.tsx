"use client";

import Link from "next/link";
import { useTranslations } from "next-intl";
import type { LoyaltyProfile } from "../model/types";
import { formatPrice } from "@radolfa/shared/lib";

/** Crown rewards icon — verbatim from the B-Magenta reference. */
function CrownIcon({ size = 13 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor">
      <path d="M5 16l-3-8 5.5 4L12 4l4.5 8L22 8l-3 8H5z" />
    </svg>
  );
}

function formatPoints(points: number): string {
  return points.toLocaleString("en-US").replace(/,/g, " ");
}

/**
 * The Crown loyalty hero card — verbatim Tailwind from the B-Magenta
 * desktop `<!-- OVERVIEW -->` block (`p-7`, `text-[52px]`, 3 perk tiles) and
 * its mobile equivalent (`p-5`, `text-[40px]`, no perk tiles), merged into a
 * single responsive component. Reused by Overview (Phase 7) and Rewards
 * (file 03, Phase 9) — depends only on `loyalty` + i18n.
 */
export default function ProfileLoyaltyHero({ loyalty }: { loyalty: LoyaltyProfile | null }) {
  const t = useTranslations("profile");

  const points = loyalty?.points ?? 0;
  const tier = loyalty?.tier ?? null;
  const spendToNextTier = loyalty?.spendToNextTier ?? null;
  const hasNextTier = tier != null && spendToNextTier != null;

  // Progress toward the next tier, based on how much more spend is required
  // relative to the spend already represented by the current point balance.
  const progressPct = hasNextTier
    ? Math.max(4, Math.min(100, Math.round((points / (points + (spendToNextTier as number))) * 100)))
    : 100;

  return (
    <div className="rounded-3xl bg-gradient-to-br from-mag to-maglo text-white p-5 lg:p-7 relative overflow-hidden">
      <div className="absolute -right-8 lg:-right-10 -top-8 lg:-top-10 w-32 lg:w-48 h-32 lg:h-48 rounded-full bg-white/10" />
      <div className="hidden lg:block absolute -right-2 bottom-0 w-32 h-32 rounded-full bg-maghi/30" />

      <div className="relative flex items-start justify-between gap-4 flex-wrap">
        <div>
          <div className="text-[10px] lg:text-[11px] tracking-[0.2em] uppercase font-bold text-[#FFCC4F] inline-flex items-center gap-1.5">
            <CrownIcon size={11} />
            {tier ? t("crownRewardsLabel", { tier: tier.name }) : t("sectionRewards")}
          </div>
          <div className="mt-1.5 lg:mt-2 flex items-baseline gap-1.5 lg:gap-2">
            <span className="text-[40px] lg:text-[52px] font-black tabular-nums leading-none">
              {formatPoints(points)}
            </span>
            <span className="text-[13px] lg:text-[15px] font-semibold opacity-80">
              {t("pointsUnit")}
            </span>
          </div>
          <div className="text-[12px] lg:text-[13px] opacity-85 mt-1">
            {tier
              ? t("loyaltyHeroSubtitle", { percent: tier.discountPercentage })
              : t("startEarning")}
          </div>
        </div>
        <Link
          href="/profile/rewards"
          className="hidden lg:flex h-11 px-6 rounded-full bg-[#FFCC4F] text-ink font-black text-[14px] shrink-0 hover:bg-white items-center justify-center"
        >
          {t("redeemPoints")}
        </Link>
      </div>

      {hasNextTier && (
        <div className="relative mt-6 hidden lg:block">
          <div className="flex items-center justify-between text-[12px] opacity-85 mb-2">
            <span className="font-bold">{tier!.name}</span>
            <span>{t("pointsToNextTier", { amount: formatPrice(spendToNextTier) })}</span>
          </div>
          <div className="h-3 rounded-full bg-white/20 overflow-hidden">
            <div className="h-full rounded-full bg-[#FFCC4F]" style={{ width: `${progressPct}%` }} />
          </div>
          <div className="mt-5 grid grid-cols-3 gap-3 text-[12px]">
            <div className="rounded-2xl bg-white/10 backdrop-blur px-4 py-3">
              <div className="font-bold">{t("perkFreeDelivery")}</div>
              <div className="opacity-75 text-[11px]">{t("perkFreeDeliveryDesc")}</div>
            </div>
            <div className="rounded-2xl bg-white/10 backdrop-blur px-4 py-3">
              <div className="font-bold">{t("perkEarlySales")}</div>
              <div className="opacity-75 text-[11px]">{t("perkEarlySalesDesc")}</div>
            </div>
            <div className="rounded-2xl bg-white/10 backdrop-blur px-4 py-3">
              <div className="font-bold">{t("perkBirthdayGift")}</div>
              <div className="opacity-75 text-[11px]">{t("perkBirthdayGiftDesc")}</div>
            </div>
          </div>
        </div>
      )}

      {hasNextTier && (
        <div className="mt-4 lg:hidden">
          <div className="flex items-center justify-between text-[11px] opacity-85 mb-1.5">
            <span className="font-bold">{tier!.name}</span>
            <span>{t("pointsToNextTier", { amount: formatPrice(spendToNextTier) })}</span>
          </div>
          <div className="h-2.5 rounded-full bg-white/20 overflow-hidden">
            <div className="h-full rounded-full bg-[#FFCC4F]" style={{ width: `${progressPct}%` }} />
          </div>
        </div>
      )}

      <Link
        href="/profile/rewards"
        className="lg:hidden mt-4 w-full h-11 rounded-full bg-[#FFCC4F] text-ink font-black text-[13px] flex items-center justify-center"
      >
        {t("redeemPoints")}
      </Link>
    </div>
  );
}
