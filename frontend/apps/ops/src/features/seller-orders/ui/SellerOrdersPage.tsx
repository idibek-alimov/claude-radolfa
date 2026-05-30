"use client";

import { ShoppingBag } from "lucide-react";
import { useTranslations } from "next-intl";

// TODO(marketplace-backend): wire to GET /api/v1/seller/orders
// with server-side page/size/search/sort per the data-query rule.
export function SellerOrdersPage() {
  const t = useTranslations("seller");

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">{t("orders.title")}</h1>
      <div className="flex flex-col items-center justify-center border border-dashed rounded-xl p-12 text-center">
        <ShoppingBag className="h-10 w-10 text-muted-foreground/40 mb-4" />
        <p className="text-sm text-muted-foreground max-w-md">
          {t("orders.empty")}
        </p>
      </div>
    </div>
  );
}
