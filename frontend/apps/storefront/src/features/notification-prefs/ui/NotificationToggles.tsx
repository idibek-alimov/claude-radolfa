"use client";

import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { Switch } from "@radolfa/shared/ui/switch";
import { Skeleton } from "@radolfa/shared/ui/skeleton";
import { getErrorMessage } from "@radolfa/shared/lib";
import type { NotificationPreferences } from "@radolfa/shared/user";
import { useNotificationPrefs, useUpdateNotificationPrefs } from "../api";

const ROWS: { key: keyof NotificationPreferences; labelKey: string; descKey: string }[] = [
  { key: "orderUpdates", labelKey: "notifOrderUpdates", descKey: "notifOrderUpdatesDesc" },
  { key: "promotions", labelKey: "notifPromotions", descKey: "notifPromotionsDesc" },
  { key: "smsMessages", labelKey: "notifSms", descKey: "notifSmsDesc" },
];

export function NotificationToggles() {
  const t = useTranslations("profile");
  const { data: prefs, isLoading } = useNotificationPrefs();
  const updateMutation = useUpdateNotificationPrefs();

  function handleToggle(key: keyof NotificationPreferences, checked: boolean) {
    if (!prefs) return;
    updateMutation.mutate(
      { ...prefs, [key]: checked },
      {
        onError: (err) => toast.error(getErrorMessage(err, t("prefsUpdateFailed"))),
      }
    );
  }

  if (isLoading || !prefs) {
    return (
      <div className="space-y-3">
        <Skeleton className="h-12 w-full rounded-xl" />
        <Skeleton className="h-12 w-full rounded-xl" />
        <Skeleton className="h-12 w-full rounded-xl" />
      </div>
    );
  }

  return (
    <div className="divide-y divide-ink/8 text-[13px]">
      {ROWS.map(({ key, labelKey, descKey }) => (
        <div key={key} className="flex items-center justify-between py-3">
          <div>
            <div className="font-bold">{t(labelKey)}</div>
            <div className="text-ink/50 text-[12px]">{t(descKey)}</div>
          </div>
          <Switch
            checked={prefs[key]}
            onCheckedChange={(checked) => handleToggle(key, checked)}
          />
        </div>
      ))}
    </div>
  );
}
