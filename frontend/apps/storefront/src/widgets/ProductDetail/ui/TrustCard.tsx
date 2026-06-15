import { Truck, RotateCcw, Award } from "lucide-react";
import { useTranslations } from "next-intl";

/**
 * Static, generically-true trust signals (delivery / returns / warranty).
 * No fabricated dates or promo copy — see updates/pdp/01-pdp-redesign.md Phase 4.
 */
export default function TrustCard() {
  const t = useTranslations("productDetail");

  return (
    <div className="rounded-2xl md:rounded-3xl bg-white border border-ink/8 p-4 md:p-5 space-y-3 text-[13px]">
      <div className="flex items-start gap-3">
        <div className="w-9 h-9 rounded-full bg-emerald/15 text-emerald flex items-center justify-center shrink-0">
          <Truck className="h-[18px] w-[18px]" />
        </div>
        <div>
          <div className="font-bold">{t("trustDeliveryTitle")}</div>
          <div className="text-[12px] text-ink/60 mt-0.5">{t("trustDeliverySub")}</div>
        </div>
      </div>
      <div className="flex items-start gap-3">
        <div className="w-9 h-9 rounded-full bg-mag/15 text-mag flex items-center justify-center shrink-0">
          <RotateCcw className="h-[18px] w-[18px]" />
        </div>
        <div>
          <div className="font-bold">{t("trustReturnsTitle")}</div>
          <div className="text-[12px] text-ink/60 mt-0.5">{t("trustReturnsSub")}</div>
        </div>
      </div>
      <div className="flex items-start gap-3">
        <div
          className="w-9 h-9 rounded-full bg-gold/20 flex items-center justify-center shrink-0"
          style={{ color: "#9A6E0F" }}
        >
          <Award className="h-[18px] w-[18px]" />
        </div>
        <div>
          <div className="font-bold">{t("trustWarrantyTitle")}</div>
          <div className="text-[12px] text-ink/60 mt-0.5">{t("trustWarrantySub")}</div>
        </div>
      </div>
    </div>
  );
}
