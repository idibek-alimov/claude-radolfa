"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { ChevronLeft, Package, Loader2, AlertCircle } from "lucide-react";
import { useTranslations } from "next-intl";
import { useAuth } from "@/features/auth";
import { fetchListingBySlug } from "@/entities/product/api";
import { GeneralInfoCard } from "./GeneralInfoCard";
import { SkuTableCard } from "./SkuTableCard";
import { EnrichmentCard } from "./EnrichmentCard";
import { ImageCard } from "./ImageCard";

interface Props {
  slug: string;
}

export function ProductEditPage({ slug }: Props) {
  const t = useTranslations("manage");
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  const { data: detail, isLoading, isError } = useQuery({
    queryKey: ["listing", slug],
    queryFn: () => fetchListingBySlug(slug),
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-24 text-muted-foreground gap-2">
        <Loader2 className="h-5 w-5 animate-spin" />
        <span>{t("loading")}</span>
      </div>
    );
  }

  if (isError || !detail) {
    return (
      <div className="flex items-center justify-center py-24 text-destructive gap-2">
        <AlertCircle className="h-5 w-5" />
        <span>{t("failedToLoad")}</span>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-muted/30 py-10">
      <div className="max-w-[1600px] mx-auto px-4 sm:px-6 lg:px-8 space-y-6">
        {/* Breadcrumb */}
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <Link
            href="/manage"
            className="flex items-center gap-1 hover:text-foreground transition-colors"
          >
            <ChevronLeft className="h-4 w-4" />
            {t("backToProducts")}
          </Link>
          <span>/</span>
          <span className="flex items-center gap-1 text-foreground font-medium truncate max-w-xs">
            <Package className="h-4 w-4 flex-shrink-0" />
            {detail.colorDisplayName}
          </span>
        </div>

        {/* Page title */}
        <div>
          <h1 className="text-2xl font-bold text-foreground">{t("editProductTitle")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            <code className="bg-muted px-1.5 py-0.5 rounded text-xs">{detail.slug}</code>
          </p>
        </div>

        {/* Cards */}
        <GeneralInfoCard detail={detail} />
        <SkuTableCard slug={slug} skus={detail.skus} isAdmin={isAdmin} />
        <EnrichmentCard detail={detail} />
        <ImageCard slug={slug} images={detail.images} />
      </div>
    </div>
  );
}
