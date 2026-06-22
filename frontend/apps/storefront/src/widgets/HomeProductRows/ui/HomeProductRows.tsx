"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Skeleton } from "@radolfa/shared/ui/skeleton";
import { fetchHomeCollections, ProductCard, type HomeSection } from "@/entities/product";

interface ProductRowProps {
  section: HomeSection;
  seeAllHref: string;
}

function ProductRow({ section, seeAllHref }: ProductRowProps) {
  const t = useTranslations("home");

  return (
    <section className="max-w-[1440px] mx-auto px-6 pt-6 md:pt-10">
      <div className="flex items-baseline justify-between mb-4">
        <h2 className="font-black text-2xl md:text-3xl">{section.title}</h2>
        <Link href={seeAllHref} className="text-mag text-[14px] font-bold hover:underline">
          {t("seeAll")}
        </Link>
      </div>
      <div className="flex gap-3 overflow-x-auto scrollbar-hide pb-2 -mx-6 px-6 md:mx-0 md:px-0 md:grid md:grid-cols-5 md:gap-4 md:overflow-visible">
        {section.listings.map((listing) => (
          <div key={listing.variantId} className="shrink-0 w-40 md:w-auto">
            <ProductCard listing={listing} />
          </div>
        ))}
      </div>
    </section>
  );
}

const ROW_CONFIG: Array<{ key: string; seeAllHref: string }> = [
  { key: "top_sellers", seeAllHref: "/collections/top_sellers" },
  { key: "new_arrivals", seeAllHref: "/collections/new_arrivals" },
];

export function HomeProductRows() {
  const { data: sections, isLoading } = useQuery({
    queryKey: ["home", "collections"],
    queryFn: fetchHomeCollections,
    staleTime: 5 * 60 * 1000,
  });

  if (isLoading) {
    return (
      <>
        {ROW_CONFIG.map((row) => (
          <section key={row.key} className="max-w-[1440px] mx-auto px-6 pt-6 md:pt-10">
            <Skeleton className="h-8 w-40 mb-4 md:h-9 md:w-48" />
            <div className="flex gap-3 overflow-x-auto scrollbar-hide pb-2 -mx-6 px-6 md:mx-0 md:px-0 md:grid md:grid-cols-5 md:gap-4 md:overflow-visible">
              {Array.from({ length: 5 }).map((_, i) => (
                <div key={i} className="shrink-0 w-40 md:w-auto">
                  <Skeleton className="rounded-2xl aspect-square w-full" />
                </div>
              ))}
            </div>
          </section>
        ))}
      </>
    );
  }

  const renderedRows = ROW_CONFIG.map((config) => ({
    config,
    section: sections?.find((s) => s.key === config.key),
  })).filter((r) => r.section && r.section.listings.length > 0);

  return (
    <>
      {renderedRows.map(({ config, section }) => (
        <ProductRow key={config.key} section={section!} seeAllHref={config.seeAllHref} />
      ))}
    </>
  );
}
