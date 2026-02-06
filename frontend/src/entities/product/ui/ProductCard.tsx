"use client";

import Image from "next/image";
import Link from "next/link";
import { motion } from "framer-motion";
import type { Product } from "@/entities/product";
import { Badge } from "@/shared/ui/badge";
import StockBadge from "./StockBadge";
import { formatPrice } from "@/shared/lib/format";

interface ProductCardProps {
  product: Product;
}

/**
 * Self-contained card that renders a single product.
 *
 * Constraints enforced here:
 *   - name, price, stock are DISPLAY-ONLY. No <input> or editable element.
 *   - Entire card links to the product detail page.
 */
export default function ProductCard({ product }: ProductCardProps) {
  const coverImage = product.images[0] ?? null;

  return (
    <Link href={`/products/${product.erpId}`} className="group block">
      <motion.div
        whileHover={{ y: -4 }}
        transition={{ duration: 0.2 }}
        className="relative rounded-xl border bg-card text-card-foreground shadow-sm hover:shadow-lg transition-shadow overflow-hidden flex flex-col h-full"
      >
        {/* Top-Seller badge */}
        {product.topSelling && (
          <Badge
            variant="warning"
            className="absolute top-3 right-3 z-10"
          >
            Top Seller
          </Badge>
        )}

        {/* Cover image */}
        <div className="relative w-full h-52 bg-muted overflow-hidden">
          {coverImage ? (
            <Image
              src={coverImage}
              alt={product.name ?? "Product image"}
              fill
              className="object-cover group-hover:scale-105 transition-transform duration-300"
              unoptimized
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center">
              <span className="text-muted-foreground text-sm">No image</span>
            </div>
          )}
        </div>

        {/* Body */}
        <div className="p-4 flex flex-col flex-1 gap-2">
          <h3 className="font-semibold text-foreground truncate">
            {product.name ?? "—"}
          </h3>

          <p className="text-sm text-muted-foreground line-clamp-2 flex-1">
            {product.webDescription ?? "No description yet."}
          </p>

          {/* Price + stock row */}
          <div className="mt-auto flex items-center justify-between pt-2">
            <span className="text-lg font-bold text-primary">
              {formatPrice(product.price)}
            </span>
            <StockBadge stock={product.stock} />
          </div>
        </div>
      </motion.div>
    </Link>
  );
}
