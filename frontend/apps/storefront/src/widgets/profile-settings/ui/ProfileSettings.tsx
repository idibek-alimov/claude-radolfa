"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Phone, ChevronRight, LogOut } from "lucide-react";
import { useAuth } from "@radolfa/shared/auth";
import { ProfileDetailsCard } from "@/features/account-settings";
import { NotificationToggles } from "@/features/notification-prefs";
import { ChangePhoneDialog } from "@/features/change-phone";

const HELP_LINKS = [
  { key: "returnsRefunds", href: "/profile/returns" },
  { key: "contactSupport", href: "#" },
  { key: "privacyData", href: "/privacy" },
] as const;

const PREFERENCE_ROWS = [
  { key: "language", valueKey: "languageEnglish" },
  { key: "currency", valueKey: "currencyValue" },
  { key: "region", valueKey: "regionValue" },
] as const;

// Phase 12 — Settings: profile details, change-phone, notification prefs,
// static preferences/help, sign out. From the `<!-- SETTINGS -->` markup.
export function ProfileSettings() {
  const t = useTranslations("profile");
  const { user, logout } = useAuth();
  const [phoneDialogOpen, setPhoneDialogOpen] = useState(false);

  return (
    <section className="space-y-5">
      <div>
        <h1 className="text-[28px] font-black leading-tight">{t("sectionSettings")}</h1>
        <p className="text-ink/55 text-[13px] mt-1">{t("settingsSubtitle")}</p>
      </div>

      <ProfileDetailsCard />

      {/* Phone number */}
      <section className="rounded-3xl bg-white border border-ink/8 p-4 lg:p-6">
        <h2 className="font-black text-lg mb-1">{t("phoneNumberTitle")}</h2>
        <p className="text-ink/55 text-[13px] mb-4">{t("changePhone")}</p>

        <div className="flex items-center justify-between gap-4 rounded-2xl bg-soft px-5 py-4">
          <div className="flex items-center gap-3 min-w-0">
            <div className="w-11 h-11 rounded-full bg-mag/12 text-mag flex items-center justify-center shrink-0">
              <Phone className="h-5 w-5" />
            </div>
            <div className="min-w-0">
              <div className="font-black tabular-nums text-[15px] truncate">{user?.phone}</div>
              <div className="text-ink/50 text-[12px]">{t("verifiedBySms")}</div>
            </div>
          </div>
          <button
            type="button"
            onClick={() => setPhoneDialogOpen(true)}
            className="h-9 px-5 rounded-full bg-mag text-white text-[13px] font-bold hover:bg-maglo shrink-0"
          >
            {t("changeNumber")}
          </button>
        </div>
      </section>

      {/* Notifications */}
      <section className="rounded-3xl bg-white border border-ink/8 p-4 lg:p-6">
        <h2 className="font-black text-lg mb-2">{t("notifications")}</h2>
        <NotificationToggles />
      </section>

      {/* Preferences & Help */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
        <section className="rounded-3xl bg-white border border-ink/8 p-4 lg:p-6">
          <h2 className="font-black text-lg mb-2">{t("preferences")}</h2>
          <div className="divide-y divide-ink/8 text-[13px]">
            {PREFERENCE_ROWS.map(({ key, valueKey }) => (
              <div key={key} className="flex items-center justify-between py-3">
                <span className="font-bold">{t(key)}</span>
                <span className="text-ink/60">{t(valueKey)}</span>
              </div>
            ))}
          </div>
        </section>

        <section className="rounded-3xl bg-white border border-ink/8 p-4 lg:p-6">
          <h2 className="font-black text-lg mb-2">{t("helpLinks")}</h2>
          <div className="divide-y divide-ink/8 text-[13px]">
            {HELP_LINKS.map(({ key, href }) => (
              <a
                key={key}
                href={href}
                className="flex items-center justify-between py-3 cursor-pointer hover:text-mag"
              >
                {t(key)}
                <ChevronRight className="h-4 w-4 text-ink/40" />
              </a>
            ))}
          </div>
        </section>
      </div>

      {/* Sign out */}
      <button
        type="button"
        onClick={logout}
        className="w-full h-12 rounded-2xl border-2 border-sale/30 text-sale font-bold text-[14px] inline-flex items-center justify-center gap-2 hover:bg-sale/5"
      >
        <LogOut className="h-4 w-4" />
        {t("signOut")}
      </button>

      <ChangePhoneDialog open={phoneDialogOpen} onOpenChange={setPhoneDialogOpen} />
    </section>
  );
}
