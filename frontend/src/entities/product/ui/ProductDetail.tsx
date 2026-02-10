"use client";

import Image from "next/image";
import Link from "next/link";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchListingBySlug } from "@/entities/product/api";
import type { Sku, ListingVariantDetail } from "@/entities/product";
import { Badge } from "@/shared/ui/badge";
import {
  Breadcrumb,
  BreadcrumbList,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/shared/ui/breadcrumb";
import StockBadge from "./StockBadge";
import ProductDetailSkeleton from "./ProductDetailSkeleton";
import { formatPrice } from "@/shared/lib/format";

interface ProductDetailProps {
  slug: string;
}

/**
 * Full-page product detail view — two-column layout.
 *
 * Features:
 *   - Image gallery with thumbnail strip
 *   - Colour swatches linking to sibling variants
 *   - Size selector with per-size stock
 *   - Dynamic price display based on selected SKU
 */
export default function ProductDetail({ slug }: ProductDetailProps) {
  const [selectedImageIdx, setSelectedImageIdx] = useState(0);
  const [selectedSku, setSelectedSku] = useState<Sku | null>(null);

  const { data: listing, isLoading, isError } = useQuery({
    queryKey: ["listing", slug],
    queryFn: () => fetchListingBySlug(slug),
    enabled: slug.length > 0,
  });

  if (isLoading) return <ProductDetailSkeleton />;

  if (isError || !listing) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16 text-center">
        <p className="text-destructive font-medium">
          Product not found or an error occurred.
        </p>
        <p className="text-muted-foreground text-sm mt-2">
          Slug: <span className="font-mono">{slug}</span>
        </p>
      </div>
    );
  }

  const mainImage =
    listing.images[selectedImageIdx] ?? listing.images[0] ?? null;

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      {/* Breadcrumb */}
      <Breadcrumb className="mb-8">
        <BreadcrumbList>
          <BreadcrumbItem>
            <BreadcrumbLink asChild>
              <Link href="/">Home</Link>
            </BreadcrumbLink>
          </BreadcrumbItem>
          <BreadcrumbSeparator />
          <BreadcrumbItem>
            <BreadcrumbLink asChild>
              <Link href="/products">Products</Link>
            </BreadcrumbLink>
          </BreadcrumbItem>
          <BreadcrumbSeparator />
          <BreadcrumbItem>
            <BreadcrumbPage>{listing.name ?? slug}</BreadcrumbPage>
          </BreadcrumbItem>
        </BreadcrumbList>
      </Breadcrumb>

      <div className="grid grid-cols-1 lg:grid-cols-5 gap-10">
        {/* Left — Image gallery (60%) */}
        <div className="lg:col-span-3">
          {/* Main image */}
          <div className="relative w-full aspect-square rounded-xl border bg-muted overflow-hidden">
            {mainImage ? (
              <Image
                src={mainImage}
                alt={`${listing.name ?? "Product"} — main`}
                fill
                className="object-cover"
                unoptimized
              />
            ) : (
              <div className="w-full h-full flex items-center justify-center">
                <span className="text-muted-foreground">
                  No image available
                </span>
              </div>
            )}
          </div>

          {/* Thumbnail strip */}
          {listing.images.length > 1 && (
            <div className="flex gap-3 mt-4 overflow-x-auto pb-2">
              {listing.images.map((url, idx) => (
                <button
                  key={url}
                  onClick={() => setSelectedImageIdx(idx)}
                  className={`relative w-20 h-20 rounded-lg border-2 overflow-hidden shrink-0 transition-colors ${idx === selectedImageIdx
                      ? "border-primary"
                      : "border-transparent hover:border-muted-foreground/30"
                    }`}
                >
                  <Image
                    src={url}
                    alt={`${listing.name ?? "Product"} — thumbnail ${idx + 1}`}
                    fill
                    className="object-cover"
                    unoptimized
                  />
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Right — Product info (40%) */}
        <div className="lg:col-span-2 space-y-5">
          <h1 className="text-3xl font-bold text-foreground">
            {listing.name ?? "—"}
          </h1>

          {/* Price */}
          <div className="space-y-1">
            {selectedSku ? (
              <div className="flex items-baseline gap-3">
                <span className="text-3xl font-semibold text-primary">
                  {formatPrice(selectedSku.salePrice)}
                </span>
                {selectedSku.onSale && (
                  <span className="text-lg text-muted-foreground line-through">
                    {formatPrice(selectedSku.price)}
                  </span>
                )}
                {selectedSku.onSale && (
                  <Badge variant="destructive" className="text-xs">
                    Sale
                  </Badge>
                )}
              </div>
            ) : (
              <p className="text-3xl font-semibold text-primary">
                {listing.priceStart === listing.priceEnd
                  ? formatPrice(listing.priceStart)
                  : `${formatPrice(listing.priceStart)} – ${formatPrice(listing.priceEnd)}`}
              </p>
            )}
          </div>

          {/* Stock */}
          <div className="flex items-center gap-3">
            <StockBadge
              stock={selectedSku ? selectedSku.stockQuantity : listing.totalStock}
            />
            {listing.colorKey && (
              <Badge variant="secondary">{listing.colorKey}</Badge>
            )}
          </div>

          {/* Colour Swatches */}
          {listing.siblingVariants && listing.siblingVariants.length > 0 && (
            <div className="pt-4 border-t">
              <h2 className="text-sm font-medium text-muted-foreground mb-3">
                Available Colours
              </h2>
              <div className="flex gap-2 flex-wrap">
                {/* Current colour (highlighted) */}
                <div className="relative w-12 h-12 rounded-lg border-2 border-primary overflow-hidden">
                  {listing.images[0] ? (
                    <Image
                      src={listing.images[0]}
                      alt={listing.colorKey}
                      fill
                      className="object-cover"
                      unoptimized
                    />
                  ) : (
                    <div className="w-full h-full bg-muted flex items-center justify-center text-xs">
                      {listing.colorKey}
                    </div>
                  )}
                </div>
                {/* Sibling colours */}
                {listing.siblingVariants.map((sibling) => (
                  <Link
                    key={sibling.slug}
                    href={`/products/${sibling.slug}`}
                    className="relative w-12 h-12 rounded-lg border-2 border-transparent hover:border-muted-foreground/50 overflow-hidden transition-colors"
                  >
                    {sibling.thumbnail ? (
                      <Image
                        src={sibling.thumbnail}
                        alt={sibling.colorKey}
                        fill
                        className="object-cover"
                        unoptimized
                      />
                    ) : (
                      <div className="w-full h-full bg-muted flex items-center justify-center text-xs">
                        {sibling.colorKey}
                      </div>
                    )}
                  </Link>
                ))}
              </div>
            </div>
          )}

          {/* Size Selector */}
          {listing.skus && listing.skus.length > 0 && (
            <div className="pt-4 border-t">
              <h2 className="text-sm font-medium text-muted-foreground mb-3">
                Select Size
              </h2>
              <div className="flex gap-2 flex-wrap">
                {listing.skus.map((sku) => {
                  const isOutOfStock = sku.stockQuantity === 0;
                  const isSelected = selectedSku?.id === sku.id;

                  return (
                    <button
                      key={sku.id}
                      onClick={() => !isOutOfStock && setSelectedSku(sku)}
                      disabled={isOutOfStock}
                      className={`px-4 py-2 rounded-lg border-2 text-sm font-medium transition-colors ${isSelected
                          ? "border-primary bg-primary text-primary-foreground"
                          : isOutOfStock
                            ? "border-muted bg-muted text-muted-foreground cursor-not-allowed line-through"
                            : "border-input hover:border-primary/50 bg-background"
                        }`}
                    >
                      {sku.sizeLabel}
                      {!isOutOfStock && (
                        <span className="ml-1.5 text-xs opacity-60">
                          ({sku.stockQuantity})
                        </span>
                      )}
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {/* Add to Cart */}
          <button
            disabled={!selectedSku}
            className={`w-full py-3 rounded-lg text-sm font-semibold transition-colors ${selectedSku
                ? "bg-primary text-primary-foreground hover:bg-primary/90"
                : "bg-muted text-muted-foreground cursor-not-allowed"
              }`}
          >
            {selectedSku ? "Add to Cart" : "Select a size"}
          </button>

          {/* Description */}
          {listing.webDescription && (
            <div className="pt-4 border-t">
              <h2 className="text-sm font-medium text-muted-foreground mb-2">
                Description
              </h2>
              <p className="text-foreground leading-relaxed">
                {listing.webDescription}
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
