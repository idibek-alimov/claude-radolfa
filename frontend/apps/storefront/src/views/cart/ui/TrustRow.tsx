import { ShieldCheck, RotateCcw, CreditCard } from "lucide-react";
import { useTranslations } from "next-intl";

export function TrustRow() {
  const t = useTranslations("cart");

  return (
    <div className="bg-white rounded-2xl border border-ink/8 p-4 grid grid-cols-1 gap-3 text-[12px]">
      <div className="flex items-center gap-2.5">
        <ShieldCheck className="h-[18px] w-[18px] text-emerald shrink-0" strokeWidth={2} />
        <span>
          <span className="font-semibold">{t("qualityGuarantee")}</span>
        </span>
      </div>
      <div className="flex items-center gap-2.5">
        <RotateCcw className="h-[18px] w-[18px] text-emerald shrink-0" strokeWidth={2} />
        <span>
          <span className="font-semibold">{t("easyReturns")}</span>
        </span>
      </div>
      <div className="flex items-center gap-2.5">
        <CreditCard className="h-[18px] w-[18px] text-emerald shrink-0" strokeWidth={2} />
        <span>
          <span className="font-semibold">{t("securePayment")}</span>
        </span>
      </div>
    </div>
  );
}
