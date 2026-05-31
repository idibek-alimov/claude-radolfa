"use client";

import { useMyProducts, useMyOrders, useMyProfile } from "@/entities/seller/api/seller";
import { Skeleton } from "@radolfa/shared/ui/skeleton";
import { Package, ShoppingBag, Store, Clock, CheckCircle, XCircle } from "lucide-react";
import { useTranslations } from "next-intl";
import { ProductStatus } from "@/entities/product/model/types";

function StatCard({
  label,
  value,
  icon: Icon,
  loading,
}: {
  label: string;
  value: number | undefined;
  icon: React.ElementType;
  loading: boolean;
}) {
  return (
    <div className="rounded-xl border bg-card p-5 shadow-sm flex items-center gap-4">
      <div className="p-2.5 rounded-lg bg-muted">
        <Icon className="h-5 w-5 text-muted-foreground" />
      </div>
      <div>
        <p className="text-xs text-muted-foreground">{label}</p>
        {loading ? (
          <Skeleton className="h-7 w-16 mt-1" />
        ) : (
          <p className="text-2xl font-bold tabular-nums">{value ?? 0}</p>
        )}
      </div>
    </div>
  );
}

export function SellerDashboard() {
  const t = useTranslations("seller");

  const { data: profile, isLoading: profileLoading } = useMyProfile();

  // Fetch product counts per status (totalElements only — size=1 to minimise payload)
  const { data: allProducts,     isLoading: allLoading }     = useMyProducts({ page: 1, size: 1 });
  const { data: draftProducts,   isLoading: draftLoading }   = useMyProducts({ page: 1, size: 1, status: ProductStatus.DRAFT });
  const { data: pendingProducts, isLoading: pendingLoading } = useMyProducts({ page: 1, size: 1, status: ProductStatus.PENDING_REVIEW });
  const { data: activeProducts,  isLoading: activeLoading }  = useMyProducts({ page: 1, size: 1, status: ProductStatus.ACTIVE });
  const { data: recentOrders,    isLoading: ordersLoading }  = useMyOrders({ page: 1, size: 1 });

  const anyLoading = profileLoading || allLoading || draftLoading || pendingLoading || activeLoading || ordersLoading;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-3">
        <Store className="h-6 w-6 text-primary" />
        <div>
          <h1 className="text-2xl font-semibold">
            {profileLoading ? (
              <Skeleton className="h-7 w-40 inline-block" />
            ) : (
              profile?.shopName ?? t("dashboard.heading")
            )}
          </h1>
          <p className="text-sm text-muted-foreground">{t("dashboard.subtitle")}</p>
        </div>
      </div>

      {/* Stats grid */}
      <div className="grid grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-4">
        <StatCard
          label={t("dashboard.stats.totalProducts")}
          value={allProducts?.totalElements}
          icon={Package}
          loading={anyLoading}
        />
        <StatCard
          label={t("dashboard.stats.drafts")}
          value={draftProducts?.totalElements}
          icon={Clock}
          loading={anyLoading}
        />
        <StatCard
          label={t("dashboard.stats.pending")}
          value={pendingProducts?.totalElements}
          icon={Store}
          loading={anyLoading}
        />
        <StatCard
          label={t("dashboard.stats.active")}
          value={activeProducts?.totalElements}
          icon={CheckCircle}
          loading={anyLoading}
        />
        <StatCard
          label={t("dashboard.stats.orderItems")}
          value={recentOrders?.totalElements}
          icon={ShoppingBag}
          loading={anyLoading}
        />
      </div>
    </div>
  );
}
