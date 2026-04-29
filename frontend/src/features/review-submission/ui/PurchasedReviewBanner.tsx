"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Pencil } from "lucide-react";
import { fetchMyDeliveredOrders } from "@/entities/order";
import { Button } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import { SubmitReviewForm } from "./SubmitReviewForm";

interface PurchasedReviewBannerProps {
  listingVariantId: number;
  slug: string;
  isAuthenticated: boolean;
}

export function PurchasedReviewBanner({
  listingVariantId,
  slug,
  isAuthenticated,
}: PurchasedReviewBannerProps) {
  const t = useTranslations("reviews.banner");
  const [open, setOpen] = useState(false);

  const { data, isLoading, isError } = useQuery({
    queryKey: ["my-delivered-orders"],
    queryFn: fetchMyDeliveredOrders,
    enabled: isAuthenticated,
  });

  if (!isAuthenticated || isLoading || isError || !data) return null;

  // Find the most-recent unreviewed delivered item matching this variant
  let matchedOrderId: number | null = null;
  let matchedSkuId: number | null = null;
  let purchaseDate: string | null = null;

  for (const order of data) {
    const item = order.items.find(
      (i) => i.listingVariantId === listingVariantId && !i.hasReviewed && i.skuId !== null
    );
    if (item) {
      matchedOrderId = order.id;
      matchedSkuId = item.skuId;
      purchaseDate = order.createdAt;
      break;
    }
  }

  if (!matchedOrderId || !matchedSkuId || !purchaseDate) return null;

  const formattedDate = new Date(purchaseDate).toLocaleDateString();

  return (
    <>
      <Card className="bg-primary/5 border-primary/20 rounded-xl p-4 flex items-center justify-between gap-3">
        <p className="text-sm text-foreground">
          {t("purchasedOn", { date: formattedDate })}
        </p>
        <Button variant="outline" size="sm" onClick={() => setOpen(true)}>
          <Pencil className="h-3.5 w-3.5 mr-1.5" />
          {t("writeReviewCta")}
        </Button>
      </Card>

      <SubmitReviewForm
        listingVariantId={listingVariantId}
        slug={slug}
        preselectedOrderId={matchedOrderId}
        preselectedSkuId={matchedSkuId}
        open={open}
        onOpenChange={setOpen}
      />
    </>
  );
}
