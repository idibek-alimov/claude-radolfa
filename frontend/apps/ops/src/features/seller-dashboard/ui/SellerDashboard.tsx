"use client";

import { Store } from "lucide-react";
import { useTranslations } from "next-intl";

export function SellerDashboard() {
  const t = useTranslations("seller");

  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] border border-dashed rounded-xl p-12 text-center">
      <Store className="h-10 w-10 text-muted-foreground/40 mb-4" />
      <h1 className="text-2xl font-semibold text-zinc-900 mb-2">
        {t("dashboard.heading")}
      </h1>
      <p className="text-sm text-muted-foreground max-w-md">
        {t("dashboard.subtitle")}
      </p>
    </div>
  );
}
