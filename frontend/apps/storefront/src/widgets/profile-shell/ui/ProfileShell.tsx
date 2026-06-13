"use client";

import { useEffect } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useTranslations } from "next-intl";
import ProtectedRoute from "@radolfa/shared/components/ProtectedRoute";
import { activeSection } from "../lib/sections";
import ProfileSidebar from "./ProfileSidebar";
import ProfileMobileHeader from "./ProfileMobileHeader";
import ProfileTabStrip from "./ProfileTabStrip";

/** Profile shell — desktop sidebar + breadcrumb, mobile magenta header + sticky
 *  tab strip. Renders `{children}` once inside a 12-col grid: the sidebar is
 *  `hidden lg:block`, so on mobile the content spans the full width. */
export default function ProfileShell({ children }: { children: React.ReactNode }) {
  const t = useTranslations("profile");
  const pathname = usePathname();
  const section = activeSection(pathname);

  // CSS-hide the inherited PromoBar + Navbar on mobile profile routes
  // (the mobile reference starts directly with the magenta header).
  // See `globals.css` for the `.profile-chrome` rules.
  useEffect(() => {
    document.body.classList.add("profile-chrome");
    return () => document.body.classList.remove("profile-chrome");
  }, []);

  return (
    <ProtectedRoute>
      {/* Mobile header + sticky section tab strip */}
      <div className="lg:hidden">
        <ProfileMobileHeader />
        <ProfileTabStrip />
      </div>

      {/* Breadcrumb (desktop only) */}
      <div className="hidden lg:block max-w-[1440px] mx-auto px-6 pt-5 text-[12px] text-ink/55">
        <Link href="/" className="hover:text-mag">
          {t("breadcrumbHome")}
        </Link>{" "}
        <span className="opacity-50">/</span>{" "}
        <span className="text-ink/80 font-semibold">{t("breadcrumbAccount")}</span>{" "}
        <span className="opacity-50">/</span>{" "}
        <span className="text-mag font-semibold">{t(section.i18nKey)}</span>
      </div>

      <main className="max-w-[1440px] mx-auto px-4 lg:px-6 pt-3 pb-8 lg:pb-12 grid grid-cols-12 gap-6">
        <div className="hidden lg:block lg:col-span-3">
          <ProfileSidebar />
        </div>
        <div className="col-span-12 lg:col-span-9">{children}</div>
      </main>
    </ProtectedRoute>
  );
}
