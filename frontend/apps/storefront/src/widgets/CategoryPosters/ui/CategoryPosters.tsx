"use client";

import Image from "next/image";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Skeleton } from "@radolfa/shared/ui/skeleton";
import { fetchCategoryTree, fetchFeaturedCategories } from "@/entities/product";
import { getCategoryTheme } from "@/entities/category";

/**
 * Normalized tile shape consumed by both the desktop grid and mobile strip —
 * lets the render stay identical whether the data came from the curated
 * featured-categories source or the auto-fallback (category tree roots).
 */
interface CategoryTile {
  key: number;
  slug: string;
  name: string;
  subtitle: string | null;
  imageUrl: string | null;
}

export function CategoryPosters() {
  const t = useTranslations("home");

  const { data: featured, isLoading: isFeaturedLoading } = useQuery({
    queryKey: ["featured-categories"],
    queryFn: fetchFeaturedCategories,
    staleTime: 30 * 60 * 1000,
  });

  const { data: categories, isLoading: isTreeLoading } = useQuery({
    queryKey: ["categories", "tree"],
    queryFn: fetchCategoryTree,
    staleTime: 30 * 60 * 1000,
  });

  const isLoading = isFeaturedLoading || isTreeLoading;

  // Curated entries (already active-only, ordered by displayOrder) win when present;
  // otherwise fall back to the first 5 root categories with gradient tiles.
  // NOTE: `categories` is the top-level tree array — it already contains only
  // roots (children are nested), so no `parentId` filter is needed here.
  const tiles: CategoryTile[] =
    featured && featured.length > 0
      ? featured.map((f) => ({
          key: f.id,
          slug: f.categorySlug,
          name: f.title ?? f.categoryName,
          subtitle: f.subtitle,
          imageUrl: f.imageUrl,
        }))
      : (categories ?? []).slice(0, 5).map((c) => ({
          key: c.id,
          slug: c.slug,
          name: c.name,
          subtitle: null,
          imageUrl: null,
        }));

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
          {tiles.map((tile) => {
            const theme = getCategoryTheme(tile.slug);
            return (
              <Link
                key={tile.key}
                href={`/categories/${tile.slug}/products`}
                className={`block rounded-2xl overflow-hidden relative aspect-[4/5] p-5 group ${
                  tile.imageUrl
                    ? "text-white"
                    : `bg-gradient-to-br ${theme.gradient} ${theme.text === "ink" ? "text-ink" : "text-white"}`
                }`}
              >
                {tile.imageUrl && (
                  <>
                    <Image
                      src={tile.imageUrl}
                      alt={tile.name}
                      fill
                      unoptimized
                      className="object-cover transition-transform duration-300 group-hover:scale-[1.04]"
                    />
                    <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-black/10 to-transparent" />
                  </>
                )}
                <div className="relative">
                  <div className="font-black text-2xl mt-1">{tile.name}</div>
                  {tile.subtitle && (
                    <div className="text-[12px] mt-1 opacity-90">{tile.subtitle}</div>
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
          {tiles.map((tile) => {
            const theme = getCategoryTheme(tile.slug);
            return (
              <Link
                key={tile.key}
                href={`/categories/${tile.slug}/products`}
                className="shrink-0 w-20 flex flex-col items-center gap-1.5"
              >
                {tile.imageUrl ? (
                  <div className="relative w-20 h-20 rounded-2xl overflow-hidden">
                    <Image
                      src={tile.imageUrl}
                      alt={tile.name}
                      fill
                      unoptimized
                      className="object-cover"
                    />
                  </div>
                ) : (
                  <div
                    className={`w-20 h-20 rounded-2xl bg-gradient-to-br ${theme.gradient} flex items-center justify-center`}
                  />
                )}
                <span className="text-[11px] font-semibold text-center leading-tight">
                  {tile.name}
                </span>
              </Link>
            );
          })}
        </div>
      </section>
    </>
  );
}
