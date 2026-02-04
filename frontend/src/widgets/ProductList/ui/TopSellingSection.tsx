"use client";

import { useQuery } from "@tanstack/react-query";
import { ProductGrid, fetchTopSellingProducts } from "@/widgets/ProductList";

/**
 * Client shell that fetches top-selling products and hands them
 * to the already-finished ProductGrid.  Exists solely because
 * useQuery is a client hook and the page file must stay a server component.
 */
export default function TopSellingSection() {
  const { data = [], isLoading } = useQuery({
    queryKey: ["products", "top-selling"],
    queryFn: fetchTopSellingProducts,
  });

  return (
    <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <h2 className="text-2xl font-bold text-gray-900 mb-6">Top Sellers</h2>
      <ProductGrid products={data} loading={isLoading} />
    </section>
  );
}
