"use client";

import Link from "next/link";
import { ArrowRight } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { ProductGrid, fetchListings } from "@/widgets/ProductList";

export default function TopSellingSection() {
  const { data, isLoading } = useQuery({
    queryKey: ["listings", "featured"],
    queryFn: () => fetchListings(1, 4),
  });

  const listings = data?.items ?? [];

  return (
    <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-14">
      <div className="flex items-center justify-between mb-8">
        <h2 className="text-2xl sm:text-3xl font-bold text-foreground">
          Featured Products
        </h2>
        <Link
          href="/products"
          className="inline-flex items-center gap-1 text-sm font-medium text-primary hover:text-primary/80 transition-colors"
        >
          View All
          <ArrowRight className="h-4 w-4" />
        </Link>
      </div>
      <ProductGrid listings={listings} loading={isLoading} />
    </section>
  );
}
