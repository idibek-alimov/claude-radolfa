import type { Metadata } from "next";
import { CatalogSection } from "@/widgets/ProductList";

export const metadata: Metadata = {
  title: "All Products — Radolfa",
  description: "Browse our full catalog of premium products.",
};

/**
 * Catalog page — server component.
 *
 * Navbar and Footer are provided by the (storefront) layout.
 */
export default function CatalogPage() {
  return <CatalogSection />;
}
