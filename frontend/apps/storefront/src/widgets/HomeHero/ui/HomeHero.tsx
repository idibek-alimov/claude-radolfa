"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";
import { Skeleton } from "@radolfa/shared/ui/skeleton";
import { fetchHomeBanners } from "@/entities/product";
import { useLoyaltyTiers } from "@/entities/loyalty";

export function HomeHero() {
  const t = useTranslations("home");

  const { data: banners, isLoading: bannersLoading } = useQuery({
    queryKey: ["home", "banner"],
    queryFn: fetchHomeBanners,
    staleTime: 5 * 60 * 1000,
  });

  const { data: tiers, isLoading: tiersLoading } = useLoyaltyTiers();

  const mainBanner = banners?.find((b) => b.slot === "MAIN");
  const welcomeBanner = banners?.find((b) => b.slot === "WELCOME");
  const topTier = tiers?.find((tier) => tier.displayOrder === 1);

  if (bannersLoading || tiersLoading) {
    return (
      <>
        <section className="max-w-[1440px] mx-auto px-6 pt-5 hidden md:grid grid-cols-3 gap-4">
          <Skeleton className="col-span-2 rounded-3xl min-h-[400px]" />
          <div className="grid grid-rows-2 gap-4">
            <Skeleton className="rounded-3xl" />
            <Skeleton className="rounded-3xl" />
          </div>
        </section>
        <section className="px-4 pt-3 md:hidden">
          <Skeleton className="rounded-2xl min-h-[200px]" />
        </section>
        <section className="px-4 pt-4 md:hidden">
          <Skeleton className="rounded-xl h-16" />
        </section>
      </>
    );
  }

  return (
    <>
      {/* ── DESKTOP: HERO POSTER + RIGHT SECONDARY ── */}
      <section className="max-w-[1440px] mx-auto px-6 pt-5 hidden md:grid grid-cols-3 gap-4">
        {/* Main card — col-span-2 */}
        <div
          className="col-span-2 rounded-3xl overflow-hidden relative bg-gradient-to-br from-mag via-mag to-maglo text-white p-12 min-h-[400px] flex flex-col justify-between"
          style={mainBanner?.bgColorHex ? { background: mainBanner.bgColorHex } : undefined}
        >
          <div>
            {mainBanner?.badgeText && (
              <div className="text-[12px] tracking-[0.2em] uppercase font-bold opacity-90">
                {mainBanner.badgeText}
              </div>
            )}
            <h1 className="font-black text-[72px] leading-[1.0] mt-4">
              {mainBanner?.title ?? "Radolfa"}
            </h1>
            {mainBanner?.subtitle && (
              <p className="mt-4 text-[16px] opacity-90 max-w-md">{mainBanner.subtitle}</p>
            )}
          </div>
          {mainBanner?.ctaLabel && mainBanner.ctaUrl && (
            <div className="flex items-center gap-4">
              <Link
                href={mainBanner.ctaUrl}
                className="h-12 px-8 rounded-full bg-white text-mag font-bold text-[15px] inline-flex items-center gap-2 shadow-lg"
              >
                {mainBanner.ctaLabel}
              </Link>
            </div>
          )}
          {/* Decorative circles */}
          <span className="absolute -right-20 -top-20 w-72 h-72 rounded-full bg-white/10 pointer-events-none" />
          <span className="absolute -right-12 -bottom-20 w-52 h-52 rounded-full bg-gold/25 pointer-events-none" />
          <span className="absolute right-32 top-12 w-20 h-20 rounded-full bg-white/15 pointer-events-none" />
        </div>

        {/* Right column */}
        <div className="grid grid-rows-2 gap-4">
          {/* Crown card */}
          <Link
            href="/loyalty"
            className="rounded-3xl overflow-hidden bg-gold/15 border-2 border-gold/30 p-6 flex flex-col justify-between hover:shadow-lg transition relative"
          >
            <div>
              <div
                className="text-[10px] tracking-[0.2em] uppercase font-bold"
                style={{ color: "#9A6E0F" }}
              >
                {t("crownTierEyebrow")}
              </div>
              <h3 className="font-black text-2xl leading-tight mt-2">
                {t("crownOffLine1", { pct: topTier?.discountPercentage ?? 12 })}
                <br />
                {t("crownOffLine2")}
              </h3>
              <p className="text-[12px] text-ink/70 mt-2 max-w-[180px]">{t("crownFromOrder")}</p>
            </div>
            <span className="h-10 px-5 rounded-full bg-ink text-gold font-bold text-[12px] w-fit inline-flex items-center gap-1.5">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
                <path d="M5 16l-3-8 5.5 4L12 4l4.5 8L22 8l-3 8H5z" />
              </svg>
              {t("crownCta")}
            </span>
          </Link>

          {/* Welcome card */}
          {welcomeBanner ? (
            <Link
              href={welcomeBanner.ctaUrl ?? "#"}
              className="rounded-3xl overflow-hidden bg-gradient-to-br from-[#0E1116] to-[#2A3140] text-white p-6 flex flex-col justify-between hover:shadow-lg transition"
            >
              <div>
                {welcomeBanner.badgeText && (
                  <div className="text-[10px] tracking-[0.2em] uppercase font-bold text-gold">
                    {welcomeBanner.badgeText}
                  </div>
                )}
                <h3 className="font-black text-2xl leading-tight mt-2">{welcomeBanner.title}</h3>
                {welcomeBanner.subtitle && (
                  <p className="text-[12px] text-white/65 mt-2">
                    Code{" "}
                    <span className="font-mono bg-white/15 px-1.5 rounded">
                      {welcomeBanner.subtitle}
                    </span>
                  </p>
                )}
              </div>
              {welcomeBanner.ctaLabel && (
                <span className="h-10 px-5 rounded-full bg-mag text-white font-bold text-[12px] w-fit inline-flex items-center">
                  {welcomeBanner.ctaLabel}
                </span>
              )}
            </Link>
          ) : (
            <div className="rounded-3xl bg-gradient-to-br from-[#0E1116] to-[#2A3140]" />
          )}
        </div>
      </section>

      {/* ── MOBILE: HERO POSTER ── */}
      <section className="px-4 pt-3 md:hidden">
        <div
          className="rounded-2xl overflow-hidden relative bg-gradient-to-br from-mag via-mag to-maglo text-white p-5 min-h-[200px] flex flex-col justify-between"
          style={mainBanner?.bgColorHex ? { background: mainBanner.bgColorHex } : undefined}
        >
          <div>
            {mainBanner?.badgeText && (
              <div className="text-[11px] tracking-[0.18em] uppercase font-bold opacity-90">
                {mainBanner.badgeText}
              </div>
            )}
            <div className="font-black text-[34px] leading-[1.02] mt-2">
              {mainBanner?.title ?? "Radolfa"}
            </div>
          </div>
          {mainBanner?.ctaLabel && mainBanner.ctaUrl && (
            <div className="flex items-center gap-3 mt-5">
              <Link
                href={mainBanner.ctaUrl}
                className="h-11 px-6 rounded-full bg-white text-mag font-bold text-[14px] inline-flex items-center gap-1.5 shadow-lg"
              >
                {mainBanner.ctaLabel}
              </Link>
            </div>
          )}
          {/* Decorative circles */}
          <span className="absolute -right-12 -top-12 w-44 h-44 rounded-full bg-white/10 pointer-events-none" />
          <span className="absolute -right-4 -bottom-8 w-28 h-28 rounded-full bg-gold/30 pointer-events-none" />
        </div>
      </section>

      {/* ── MOBILE: LOYALTY STRIP ── */}
      <section className="px-4 pt-4 md:hidden">
        <Link
          href="/loyalty"
          className="flex items-center gap-3 bg-gold/15 border border-gold/35 rounded-xl px-4 py-3"
        >
          <div className="w-10 h-10 rounded-full bg-gold flex items-center justify-center shrink-0">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="#1A0A18" aria-hidden>
              <path d="M5 16l-3-8 5.5 4L12 4l4.5 8L22 8l-3 8H5z" />
            </svg>
          </div>
          <div className="flex-1 min-w-0">
            <div className="text-[13px] font-bold">
              {t("loyaltyStripTitle", { pct: topTier?.discountPercentage ?? 12 })}
            </div>
            <div className="text-[11px] text-ink/65 leading-tight mt-0.5">
              {t("loyaltyStripSubtext")}
            </div>
          </div>
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            aria-hidden
          >
            <polyline points="9 18 15 12 9 6" />
          </svg>
        </Link>
      </section>
    </>
  );
}
