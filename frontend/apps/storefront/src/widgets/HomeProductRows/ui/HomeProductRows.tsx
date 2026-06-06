"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Skeleton } from "@radolfa/shared/ui/skeleton";
import { fetchHomeCollections, ProductCard, type HomeSection } from "@/entities/product";

interface ProductRowProps {
  section: HomeSection;
  seeAllHref: string;
  isLast?: boolean;
}

function ProductRow({ section, seeAllHref, isLast }: ProductRowProps) {
  const t = useTranslations("home");

  return (
    <section className={`max-w-[1440px] mx-auto px-6 pt-10${isLast ? " pb-12" : ""}`}>
      <div className="flex items-baseline justify-between mb-4">
        <h2 className="font-black text-3xl">{section.title}</h2>
        <Link href={seeAllHref} className="text-mag text-[14px] font-bold hover:underline">
          {t("seeAll")}
        </Link>
      </div>
      <div className="grid grid-cols-2 gap-3 md:grid-cols-5 md:gap-4">
        {section.listings.map((listing) => (
          <ProductCard key={listing.variantId} variant="home" listing={listing} />
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
          <section key={row.key} className="max-w-[1440px] mx-auto px-6 pt-10">
            <Skeleton className="h-9 w-48 mb-4" />
            <div className="grid grid-cols-2 gap-3 md:grid-cols-5 md:gap-4">
              {Array.from({ length: 5 }).map((_, i) => (
                <Skeleton key={i} className="rounded-2xl aspect-square" />
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
      {renderedRows.map(({ config, section }, idx) => (
        <ProductRow
          key={config.key}
          section={section!}
          seeAllHref={config.seeAllHref}
          isLast={idx === renderedRows.length - 1}
        />
      ))}
    </>
  );
}
