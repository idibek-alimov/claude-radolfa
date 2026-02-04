import Image from "next/image";
import type { Product } from "@/entities/product";

interface ProductCardProps {
  product: Product;
}

/**
 * Self-contained card that renders a single product.
 *
 * Constraints enforced here:
 *   – name, price, stock are DISPLAY-ONLY.  No <input> or editable element
 *     is rendered for any of those fields.
 *   – The first image in the array is used as the cover; a grey placeholder
 *     is shown when the list is empty.
 */
export default function ProductCard({ product }: ProductCardProps) {
  const coverImage = product.images[0] ?? null;

  return (
    <div className="relative rounded-xl border bg-white shadow-sm hover:shadow-md transition-shadow overflow-hidden flex flex-col">
      {/* ── Top-Seller badge ────────────────────────────────────── */}
      {product.topSelling && (
        <span className="absolute top-2 right-2 z-10 bg-amber-400 text-amber-900 text-xs font-bold px-2 py-0.5 rounded-full">
          Top Seller
        </span>
      )}

      {/* ── Cover image ─────────────────────────────────────────── */}
      <div className="relative w-full h-48 bg-gray-100">
        {coverImage ? (
          <Image
            src={coverImage}
            alt={product.name ?? "Product image"}
            fill
            className="object-cover"
            unoptimized
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center">
            <span className="text-gray-400 text-sm">No image</span>
          </div>
        )}
      </div>

      {/* ── Body ────────────────────────────────────────────────── */}
      <div className="p-4 flex flex-col flex-1 gap-2">
        <h3 className="font-semibold text-gray-900 truncate">
          {product.name ?? "—"}
        </h3>

        <p className="text-sm text-gray-500 line-clamp-2 flex-1">
          {product.webDescription ?? "No description yet."}
        </p>

        {/* ── Price + stock row ─────────────────────────────────── */}
        <div className="mt-auto flex items-center justify-between">
          <span className="text-lg font-bold text-indigo-600">
            {product.price != null ? `$${product.price.toFixed(2)}` : "—"}
          </span>

          <span
            className={`text-xs font-medium px-2 py-0.5 rounded-full ${
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
      </div>
    </div>
  );
}
