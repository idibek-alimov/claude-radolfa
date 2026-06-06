"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Skeleton } from "@radolfa/shared/ui/skeleton";
import { fetchCategoryTree } from "@/entities/product";
import { getCategoryTheme } from "@/entities/category";

export function CategoryPosters() {
  const t = useTranslations("home");

  const { data: categories, isLoading } = useQuery({
    queryKey: ["categories", "tree"],
    queryFn: fetchCategoryTree,
    staleTime: 30 * 60 * 1000,
  });

  const rootCategories = categories?.filter((c) => c.parentId === null).slice(0, 5) ?? [];

  if (isLoading) {
    return (
      <>
        {/* Desktop skeleton */}
        <section className="max-w-[1440px] mx-auto px-6 pt-8 hidden md:block">
          <Skeleton className="h-9 w-48 mb-5" />
          <div className="grid grid-cols-5 gap-4">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="rounded-2xl aspect-[4/5]" />
            ))}
          </div>
        </section>
        {/* Mobile skeleton */}
        <section className="pt-5 pl-4 md:hidden">
          <Skeleton className="h-5 w-28 mb-3" />
          <div className="flex gap-3">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="shrink-0 w-20 h-20 rounded-2xl" />
            ))}
          </div>
        </section>
      </>
    );
  }

  return (
    <>
      {/* ── DESKTOP: BIG CATEGORY POSTERS — 5-up ── */}
      <section className="max-w-[1440px] mx-auto px-6 pt-8 hidden md:block">
        <div className="flex items-baseline justify-between mb-5">
          <h2 className="font-black text-3xl">{t("shopByCategory")}</h2>
        </div>
        <div className="grid grid-cols-5 gap-4">
          {rootCategories.map((cat) => {
            const theme = getCategoryTheme(cat.slug);
            return (
              <Link
                key={cat.id}
                href={`/categories/${cat.slug}/products`}
                className={`block rounded-2xl overflow-hidden relative aspect-[4/5] bg-gradient-to-br ${theme.gradient} ${theme.text === "ink" ? "text-ink" : "text-white"} p-5 group`}
              >
                <div className="relative">
                  <div className="text-[10px] tracking-[0.2em] uppercase opacity-80">
                    {t("itemCount", { count: cat.productCount })}
                  </div>
                  <div className="font-black text-2xl mt-1">{cat.name}</div>
                  {cat.minPrice != null && (
                    <div className="text-[11px] mt-2 opacity-90">
                      {t("fromPrice", { price: cat.minPrice })}
                    </div>
                  )}
                </div>
              </Link>
            );
          })}
        </div>
      </section>

      {/* ── MOBILE: CATEGORIES STRIP ── */}
      <section className="pt-5 pl-4 md:hidden">
        <h2 className="font-extrabold text-base mb-3">{t("categories")}</h2>
        <div className="flex gap-3 overflow-x-auto scrollbar-hide pb-2 pr-4">
          {rootCategories.map((cat) => {
            const theme = getCategoryTheme(cat.slug);
            return (
              <Link
                key={cat.id}
                href={`/categories/${cat.slug}/products`}
                className="shrink-0 w-20 flex flex-col items-center gap-1.5"
              >
                <div
                  className={`w-20 h-20 rounded-2xl bg-gradient-to-br ${theme.gradient} flex items-center justify-center`}
                />
                <span className="text-[11px] font-semibold text-center leading-tight">
                  {cat.name}
                </span>
              </Link>
            );
          })}
        </div>
      </section>
    </>
  );
}
