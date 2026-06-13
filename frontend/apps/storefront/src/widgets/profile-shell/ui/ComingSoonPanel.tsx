"use client";

import { Sparkles } from "lucide-react";
import { useTranslations } from "next-intl";

/** Design-system empty state for the route stubs (Rewards/Addresses/Payment/Settings/Orders)
 *  scaffolded in this phase. Never renders mockup sample data — real content lands in
 *  Phases 7-8 and file 03. `titleKey`/`messageKey` are `profile` namespace i18n keys. */
export default function ComingSoonPanel({
  titleKey,
  messageKey,
}: {
  titleKey: string;
  messageKey: string;
}) {
  const t = useTranslations("profile");

  return (
    <section className="space-y-4">
      <h1 className="text-[30px] font-black leading-tight">{t(titleKey)}</h1>
      <div className="border border-dashed border-ink/15 rounded-xl p-12 flex flex-col items-center text-center gap-3">
        <Sparkles className="h-10 w-10 text-ink/20" />
        <p className="text-sm text-ink/55">{t(messageKey)}</p>
      </div>
    </section>
  );
}
