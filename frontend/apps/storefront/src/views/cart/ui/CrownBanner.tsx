import { Crown } from "lucide-react";
import { useTranslations } from "next-intl";

interface CrownBannerProps {
  percent: number;
}

export function CrownBanner({ percent }: CrownBannerProps) {
  const t = useTranslations("cart");

  return (
    <div className="rounded-2xl bg-gold/15 border border-gold/35 p-3.5 sm:p-4 flex items-start gap-3">
      <div className="w-9 h-9 rounded-full bg-gold flex items-center justify-center shrink-0">
        <Crown className="h-[18px] w-[18px] text-ink" fill="currentColor" stroke="none" />
      </div>
      <div className="text-[12px] leading-snug">
        <div className="font-bold text-[13px]">{t("crownTierBanner", { percent })}</div>
        <div className="text-ink/65 mt-0.5">{t("crownTierBannerBody")}</div>
      </div>
    </div>
  );
}
