"use client";

import Image from "next/image";
import Link from "next/link";
import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchProductByErpId } from "@/entities/product";
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
import { formatPrice, formatDate } from "@/shared/lib/format";

interface ProductDetailProps {
  erpId: string;
}

/**
 * Full-page product detail view — two-column layout.
 *
 * Constraints enforced here:
 *   - name, price, stock are rendered in read-only elements only.
 *   - Every image comes from S3 via next/image with unoptimized.
 */
export default function ProductDetail({ erpId }: ProductDetailProps) {
  const [selectedIdx, setSelectedIdx] = useState(0);

  const { data: product, isLoading, isError } = useQuery({
    queryKey: ["product", erpId],
    queryFn: () => fetchProductByErpId(erpId),
    enabled: erpId.length > 0,
  });

  if (isLoading) return <ProductDetailSkeleton />;

  if (isError || !product) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16 text-center">
        <p className="text-destructive font-medium">
          Product not found or an error occurred.
        </p>
        <p className="text-muted-foreground text-sm mt-2">
          ERP ID: <span className="font-mono">{erpId}</span>
        </p>
      </div>
    );
  }

  const mainImage = product.images[selectedIdx] ?? product.images[0] ?? null;

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
            <BreadcrumbPage>{product.name ?? erpId}</BreadcrumbPage>
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
                alt={`${product.name ?? "Product"} — main`}
                fill
                className="object-cover"
                unoptimized
              />
            ) : (
              <div className="w-full h-full flex items-center justify-center">
                <span className="text-muted-foreground">No image available</span>
              </div>
            )}
          </div>

          {/* Thumbnail strip */}
          {product.images.length > 1 && (
            <div className="flex gap-3 mt-4 overflow-x-auto pb-2">
              {product.images.map((url, idx) => (
                <button
                  key={url}
                  onClick={() => setSelectedIdx(idx)}
                  className={`relative w-20 h-20 rounded-lg border-2 overflow-hidden shrink-0 transition-colors ${
                    idx === selectedIdx
                      ? "border-primary"
                      : "border-transparent hover:border-muted-foreground/30"
                  }`}
                >
                  <Image
                    src={url}
                    alt={`${product.name ?? "Product"} — thumbnail ${idx + 1}`}
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
            {product.name ?? "—"}
          </h1>

          <p className="text-3xl font-semibold text-primary">
            {formatPrice(product.price)}
          </p>

          <div className="flex items-center gap-3">
            <StockBadge stock={product.stock} />
            {product.topSelling && (
              <Badge variant="warning">Top Seller</Badge>
            )}
          </div>

          {product.webDescription && (
            <div className="pt-4 border-t">
              <h2 className="text-sm font-medium text-muted-foreground mb-2">
                Description
              </h2>
              <p className="text-foreground leading-relaxed">
                {product.webDescription}
              </p>
            </div>
          )}

          {/* Audit metadata */}
          {product.lastErpSyncAt && (
            <p className="text-xs text-muted-foreground pt-4 border-t">
              Last synced from ERP:{" "}
              <span className="font-mono">{formatDate(product.lastErpSyncAt)}</span>
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
