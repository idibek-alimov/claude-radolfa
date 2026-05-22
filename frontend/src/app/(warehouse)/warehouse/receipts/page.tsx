"use client";

import { useTranslations } from "next-intl";

export default function ReceiptsPage() {
  const t = useTranslations("warehouse");
  return (
    <div>
      <h1 className="text-2xl font-semibold text-zinc-900">{t("nav.receipts")}</h1>
      <p className="mt-2 text-sm text-zinc-500">{t("common.placeholderBody")}</p>
    </div>
  );
}
