"use client";

import Image from "next/image";
import { useQuery } from "@tanstack/react-query";
import { fetchProductByErpId } from "@/entities/product";

interface ProductDetailProps {
  /** The stable ERP identifier passed down from the dynamic route segment. */
  erpId: string;
}

/**
 * Full-page product detail view.
 *
 * Constraints enforced here (mirrors ProductCard contract):
 *   - name, price, stock are rendered in read-only elements only.
 *     Zero <input>, <textarea>, or contentEditable anywhere in this tree.
 *   - Every image comes from S3 and is rendered via next/image with
 *     explicit width/height (thumbnail gallery style).
 */
export default function ProductDetail({ erpId }: ProductDetailProps) {
  const { data: product, isLoading, isError } = useQuery({
    queryKey: ["product", erpId],
    queryFn: () => fetchProductByErpId(erpId),
    enabled: erpId.length > 0,
  });

  // ── Loading state ─────────────────────────────────────────────────
  if (isLoading) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16 text-center">
        <p className="text-gray-500 animate-pulse">Loading product…</p>
      </div>
    );
  }

  // ── Error state ───────────────────────────────────────────────────
  if (isError || !product) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16 text-center">
        <p className="text-red-600 font-medium">
          Product not found or an error occurred.
        </p>
        <p className="text-gray-500 text-sm mt-2">
          ERP ID: <span className="font-mono">{erpId}</span>
        </p>
      </div>
    );
  }

  // ── Success state ─────────────────────────────────────────────────
  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      {/* ── Image gallery ───────────────────────────────────────── */}
      {product.images.length > 0 && (
        <div className="flex flex-wrap gap-3 mb-8">
          {product.images.map((url, idx) => (
            <div
              key={url}
              className="w-32 h-32 rounded-lg border border-gray-200 overflow-hidden bg-gray-100"
            >
              <Image
                src={url}
                alt={`${product.name ?? "Product"} — image ${idx + 1}`}
                width={128}
                height={128}
                className="object-cover w-full h-full"
                unoptimized
              />
            </div>
          ))}
        </div>
      )}

      {/* ── Name (read-only) ────────────────────────────────────── */}
      <h1 className="text-3xl font-bold text-gray-900 mb-2">
        {product.name ?? "—"}
      </h1>

      {/* ── Price (read-only) ───────────────────────────────────── */}
      <p className="text-2xl font-semibold text-indigo-600 mb-4">
        {product.price != null ? `$${product.price.toFixed(2)}` : "—"}
      </p>

      {/* ── Stock badge (read-only) ─────────────────────────────── */}
      <div className="mb-6">
        <span
          className={`inline-block text-sm font-medium px-3 py-1 rounded-full ${
            (product.stock ?? 0) > 0
              ? "bg-green-100 text-green-700"
              : "bg-red-100 text-red-700"
          }`}
        >
          {(product.stock ?? 0) > 0
            ? `${product.stock} in stock`
            : "Out of stock"}
        </span>
      </div>

      {/* ── Web description ─────────────────────────────────────── */}
      {product.webDescription && (
        <p className="text-gray-700 leading-relaxed">
          {product.webDescription}
        </p>
      )}

      {/* ── Audit metadata ──────────────────────────────────────── */}
      {product.lastErpSyncAt && (
        <p className="text-xs text-gray-400 mt-8">
          Last synced from ERP:{" "}
          <span className="font-mono">
            {new Date(product.lastErpSyncAt).toLocaleString()}
          </span>
        </p>
      )}
    </div>
  );
}
