"use client";

import { useTranslations } from "next-intl";
import { useAuth } from "@radolfa/shared/auth";

// Minimal Overview placeholder for Phase 6 (shell only). The full dashboard —
// ProfileLoyaltyHero, stat tiles, recent orders, addresses preview — is built
// in Phase 7. The previous combined Account/Orders/Loyalty/Returns tabs are
// migrated to their dedicated routes (/profile/orders, /profile/rewards, ...,
// /profile/returns) as those phases land.
export default function ProfileOverviewPage() {
  const t = useTranslations("profile");
  const { user } = useAuth();

  const firstName = user?.name?.split(" ")[0] || user?.phone || "";

  return (
    <section className="space-y-6">
      <h1 className="text-[30px] font-black leading-tight">
        {t("greeting", { name: firstName })}
      </h1>
      <div className="border border-dashed border-ink/15 rounded-xl p-12 flex flex-col items-center text-center gap-3">
        <p className="text-sm text-ink/55">{t("comingSoon")}</p>
      </div>
    </section>
  );
}
