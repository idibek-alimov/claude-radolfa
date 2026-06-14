"use client";

import { ShoppingBag, PencilLine, UserPlus, Sparkles } from "lucide-react";
import { useTranslations } from "next-intl";
import { useAuth } from "@radolfa/shared/auth";
import { ProfileLoyaltyHero } from "@/entities/loyalty";

const EARN_CARDS = [
  { icon: ShoppingBag, titleKey: "earnShopTitle", descKey: "earnShopDesc" },
  { icon: PencilLine, titleKey: "earnReviewTitle", descKey: "earnReviewDesc" },
  { icon: UserPlus, titleKey: "earnReferTitle", descKey: "earnReferDesc" },
] as const;

/** Dashed empty state — design-system "coming soon" / no-data block. */
function EmptyBlock({ message }: { message: string }) {
  return (
    <div className="border border-dashed border-ink/15 rounded-xl p-12 flex flex-col items-center text-center gap-3">
      <Sparkles className="h-10 w-10 text-ink/20" />
      <p className="text-sm text-ink/55">{message}</p>
    </div>
  );
}

// Phase 9 — real Crown Rewards dashboard: loyalty hero (reused from Overview),
// static "Ways to earn" cards, and points activity from recentEarnings.
export default function ProfileRewardsPage() {
  const t = useTranslations("profile");
  const { user } = useAuth();
  const loyalty = user?.loyalty ?? null;

  const recentEarnings = (loyalty?.recentEarnings ?? [])
    .slice()
    .sort((a, b) => b.orderedAt.localeCompare(a.orderedAt));

  return (
    <section className="space-y-6">
      <h1 className="text-[28px] font-black leading-tight">{t("sectionRewards")}</h1>

      <ProfileLoyaltyHero loyalty={loyalty} />

      {/* Ways to earn */}
      <div>
        <h2 className="font-black text-lg mb-3">{t("waysToEarn")}</h2>
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-2.5 lg:gap-4">
          {EARN_CARDS.map(({ icon: Icon, titleKey, descKey }) => (
            <div
              key={titleKey}
              className="flex items-center gap-3 rounded-2xl lg:rounded-3xl bg-soft p-3 lg:p-5 lg:flex-col lg:items-start"
            >
              <div className="w-9 h-9 lg:w-10 lg:h-10 rounded-xl lg:rounded-2xl bg-mag/10 text-mag flex items-center justify-center shrink-0 lg:mb-3">
                <Icon className="h-[18px] w-[18px] lg:h-5 lg:w-5" />
              </div>
              <div className="flex-1 lg:flex-initial">
                <div className="font-bold text-[14px]">{t(titleKey)}</div>
                <div className="text-ink/55 text-[11px] lg:text-[12px] lg:mt-0.5">{t(descKey)}</div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Points activity */}
      <section className="rounded-3xl bg-white border border-ink/8 p-4 lg:p-6">
        <h2 className="font-black text-lg mb-4">{t("pointsActivity")}</h2>

        {recentEarnings.length === 0 ? (
          <EmptyBlock message={t("earnPoints")} />
        ) : (
          <div className="divide-y divide-ink/8 text-[13px]">
            {recentEarnings.map((entry) => (
              <div key={entry.orderId} className="flex items-center justify-between py-3">
                <div>
                  <div className="font-bold">{t("orderNumber", { id: entry.orderId })}</div>
                  <div className="text-ink/50 text-[12px]">
                    {new Date(entry.orderedAt).toLocaleDateString()}
                  </div>
                </div>
                <div className="font-black tabular-nums text-emerald">+{entry.pointsEarned}</div>
              </div>
            ))}
          </div>
        )}
      </section>
    </section>
  );
}
