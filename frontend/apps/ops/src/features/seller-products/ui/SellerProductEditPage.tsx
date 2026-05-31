"use client";

import { useRouter } from "next/navigation";
import Image from "next/image";
import {
  useMyProductCard,
  useSubmitMyProductForReview,
  useUpdateMySkuPrice,
  useUpdateMySkuStock,
} from "@/entities/seller/api/seller";
import { ProductStatusBadge } from "@/entities/product/ui/ProductStatusBadge";
import { ProductStatus } from "@/entities/product/model/types";
import { SkuTableCard } from "@/features/product-edit/ui/SkuTableCard";
import { Button } from "@radolfa/shared/ui/button";
import { Skeleton } from "@radolfa/shared/ui/skeleton";
import {
  Lock,
  ArrowLeft,
  Send,
  Loader2,
  Package,
  Image as ImageIcon,
} from "lucide-react";
import { useTranslations } from "next-intl";
import { getErrorMessage } from "@radolfa/shared/lib";
import { toast } from "sonner";
import { useQueryClient } from "@tanstack/react-query";

interface Props {
  productBaseId: number;
}

export function SellerProductEditPage({ productBaseId }: Props) {
  const t = useTranslations("seller");
  const router = useRouter();
  const queryClient = useQueryClient();

  const { data: card, isLoading, isError } = useMyProductCard(productBaseId);
  const submitForReview = useSubmitMyProductForReview();
  const updatePrice = useUpdateMySkuPrice();
  const updateStock = useUpdateMySkuStock();

  if (isLoading) {
    return (
      <div className="space-y-4 p-6">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-40 w-full" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  if (isError || !card) {
    return (
      <div className="p-6 text-center text-muted-foreground">
        <Package className="mx-auto h-10 w-10 mb-3 text-muted-foreground/40" />
        <p>Product not found or you do not own it.</p>
        <Button variant="outline" className="mt-4" onClick={() => router.back()}>
          Go back
        </Button>
      </div>
    );
  }

  async function handleSkuSave(skuId: number, price: number, stock: number) {
    try {
      await Promise.all([
        updatePrice.mutateAsync({ skuId, price }),
        updateStock.mutateAsync({ skuId, payload: { quantity: stock } }),
      ]);
      queryClient.invalidateQueries({ queryKey: ["seller-product", productBaseId] });
      toast.success(t("products.skuSaved"));
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  }

  const canSubmit =
    card.status === ProductStatus.DRAFT || card.status === ProductStatus.REJECTED;

  return (
    <div className="space-y-6 max-w-4xl">
      {/* Back + title */}
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon" onClick={() => router.push("/seller/products")}>
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <div className="flex-1">
          <h1 className="text-2xl font-semibold">{card.name}</h1>
          <div className="flex items-center gap-2 mt-1">
            <ProductStatusBadge status={card.status} />
            {canSubmit && (
              <Button
                size="sm"
                variant="outline"
                className="text-blue-700 border-blue-200 hover:bg-blue-50 ml-2"
                onClick={() => submitForReview.mutate(productBaseId)}
                disabled={submitForReview.isPending}
              >
                {submitForReview.isPending ? (
                  <Loader2 className="h-3.5 w-3.5 mr-1 animate-spin" />
                ) : (
                  <Send className="h-3.5 w-3.5 mr-1" />
                )}
                {t("products.submitForReview")}
              </Button>
            )}
          </div>
        </div>
      </div>

      {/* Read-only base info */}
      <div className="bg-muted/30 rounded-xl p-5 space-y-3">
        <div className="flex items-center gap-2 text-sm font-semibold text-muted-foreground uppercase tracking-wider">
          <Lock className="h-3.5 w-3.5" />
          System-managed
        </div>
        <div className="grid grid-cols-2 gap-4 text-sm">
          <div>
            <p className="text-muted-foreground">Category</p>
            <p className="font-medium">{card.categoryName ?? "—"}</p>
          </div>
          <div>
            <p className="text-muted-foreground">Brand</p>
            <p className="font-medium">{card.brand ?? "—"}</p>
          </div>
        </div>
      </div>

      {/* Variants */}
      {card.variants.map((variant) => (
        <div
          key={variant.variantId}
          className="rounded-xl border bg-card shadow-sm overflow-hidden"
        >
          {/* Variant header */}
          <div className="flex items-center gap-3 px-5 py-4 border-b">
            <div
              className="h-5 w-5 rounded-full border shadow-sm flex-shrink-0"
              style={{ backgroundColor: variant.colorHex ?? "#e5e7eb" }}
            />
            <span className="font-medium text-sm">{variant.colorDisplayName}</span>
            <span className="text-xs text-muted-foreground ml-auto">{variant.productCode}</span>
          </div>

          {/* Images (read-only) */}
          {variant.images.length > 0 && (
            <div className="px-5 py-4 border-b">
              <div className="flex items-center gap-2 text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">
                <ImageIcon className="h-3.5 w-3.5" />
                <span>Images</span>
                <Lock className="h-3 w-3 ml-1" />
              </div>
              <div className="flex gap-2 flex-wrap">
                {variant.images.map((img) => (
                  <div
                    key={img.id}
                    className="relative h-16 w-16 rounded-lg border overflow-hidden bg-muted"
                  >
                    <Image
                      src={img.url}
                      alt=""
                      fill
                      className="object-cover"
                      unoptimized
                    />
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* SKUs — price/stock editable via SkuTableCard seller mode */}
          <div className="p-5">
            <SkuTableCard
              slug={variant.slug}
              productBaseId={productBaseId}
              variantId={variant.variantId}
              skus={variant.skus}
              isAdmin={false}
              onSellerSave={handleSkuSave}
            />
          </div>
        </div>
      ))}
    </div>
  );
}
