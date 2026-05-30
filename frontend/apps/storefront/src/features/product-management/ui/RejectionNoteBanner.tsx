"use client";

import { AlertCircle } from "lucide-react";
import { useTranslations } from "next-intl";

interface Props {
  reason: string;
}

export function RejectionNoteBanner({ reason }: Props) {
  const t = useTranslations("manage.products.rejection");

  return (
    <div className="mx-8 mt-4 flex items-start gap-3 rounded-xl border border-rose-200 bg-rose-50 p-4 text-rose-900">
      <AlertCircle className="mt-0.5 h-4 w-4 shrink-0 text-rose-600" />
      <p className="text-sm">{t("banner", { reason })}</p>
    </div>
  );
}
