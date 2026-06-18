import { Truck } from "lucide-react";
import { useTranslations } from "next-intl";

export function FreeShippingBanner() {
  const t = useTranslations("cart");

  return (
    <div className="bg-white rounded-2xl border border-ink/8 p-4 sm:p-5">
      <div className="flex items-center gap-2 text-[12px] sm:text-[13px] font-semibold">
        <Truck className="h-4 sm:h-[18px] w-4 sm:w-[18px] text-emerald" strokeWidth={2} />
        <span className="text-emerald">{t("freeShippingUnlocked")}</span>
        <span className="text-ink/55 font-normal hidden sm:inline">{t("freeShippingTail")}</span>
      </div>
      <div className="mt-2.5 sm:mt-3 h-2 rounded-full bg-plum/50 overflow-hidden">
        <div className="h-full w-full bg-gradient-to-r from-mag to-maghi rounded-full" />
      </div>
    </div>
  );
}
