import { Store } from "lucide-react";
import { useTranslations } from "next-intl";

export function SellerGroupHeader() {
  const t = useTranslations("cart");

  return (
    <div className="flex items-center gap-2 px-1 pt-1">
      <Store className="h-[14px] sm:h-[15px] w-[14px] sm:w-[15px] text-ink/55" strokeWidth={2} />
      <span className="text-[13px] font-bold">{t("sellerName")}</span>
      <span className="text-[11px] sm:text-[12px] text-ink/45">· {t("dispatchCopy")}</span>
    </div>
  );
}
