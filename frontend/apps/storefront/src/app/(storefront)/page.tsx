import type { Metadata } from "next";
import { Suspense } from "react";
import { Skeleton } from "@radolfa/shared/ui/skeleton";
import { HomeHero } from "@/widgets/HomeHero";
import { CategoryPosters } from "@/widgets/CategoryPosters";
import { HomeProductRows } from "@/widgets/HomeProductRows";
import { HomeInfiniteFeed } from "@/widgets/HomeInfiniteFeed";

export const metadata: Metadata = {
  title: "Radolfa — Premium E-Commerce",
  description: "Shop top-quality products at Radolfa. Trusted marketplace with secure payments and fast delivery.",
};

function HomeInfiniteFeedFallback() {
  return (
    <section className="max-w-[1440px] mx-auto px-6 pt-10 pb-12">
      <Skeleton className="h-9 w-48 mb-4" />
      <div className="grid grid-cols-2 gap-3 md:grid-cols-5 md:gap-4">
        {Array.from({ length: 10 }).map((_, i) => (
          <Skeleton key={i} className="rounded-2xl aspect-square" />
        ))}
      </div>
    </section>
  );
}

export default function HomePage() {
  return (
    <>
      <HomeHero />
      <CategoryPosters />
      <HomeProductRows />
      <Suspense fallback={<HomeInfiniteFeedFallback />}>
        <HomeInfiniteFeed />
      </Suspense>
    </>
  );
}
