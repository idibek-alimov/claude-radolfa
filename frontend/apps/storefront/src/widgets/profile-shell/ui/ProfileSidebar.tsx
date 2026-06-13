"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useTranslations } from "next-intl";
import { useAuth } from "@radolfa/shared/auth";
import { useOrderSummary } from "@/features/profile/api";
import { PROFILE_SECTIONS, isSectionActive } from "../lib/sections";
import { getInitials } from "../lib/initials";
import { SignOutIcon, RewardsIcon } from "../lib/icons";

/** Desktop left sidebar: profile chip card + nav list + sign-out.
 *  Verbatim from the B-Magenta desktop reference `<!-- SIDEBAR -->`. */
export default function ProfileSidebar() {
  const t = useTranslations("profile");
  const pathname = usePathname();
  const { user, logout } = useAuth();
  const { data: summary } = useOrderSummary();

  const initials = getInitials(user?.name, user?.phone);
  const tier = user?.loyalty?.tier;

  return (
    <aside className="col-span-12 lg:col-span-3">
      <div className="sticky top-28 space-y-4">
        {/* Profile chip card */}
        <div className="rounded-3xl bg-soft p-5 flex items-center gap-3">
          <div className="w-14 h-14 rounded-full bg-mag/15 text-mag flex items-center justify-center text-[18px] font-black shrink-0">
            {initials}
          </div>
          <div className="min-w-0">
            <div className="font-black text-[16px] leading-tight truncate">
              {user?.name || t("yourProfile")}
            </div>
            {tier && (
              <span
                className="mt-1 inline-flex px-2 py-0.5 rounded-full bg-[#FFCC4F]/20 text-[10px] font-bold items-center gap-1"
                style={{ color: "#9A6E0F" }}
              >
                <RewardsIcon size={10} />
                Crown {tier.name}
              </span>
            )}
          </div>
        </div>

        {/* Nav list */}
        <nav className="rounded-3xl bg-white border border-ink/8 p-2 text-[14px] font-semibold">
          {PROFILE_SECTIONS.map((section) => {
            const active = isSectionActive(section, pathname);
            const Icon = section.icon;
            return (
              <Link
                key={section.seg}
                href={section.href}
                className={`flex items-center gap-3 px-4 py-[0.7rem] rounded-2xl transition-colors ${
                  active ? "bg-mag text-white" : "text-ink/72 hover:bg-plum/55"
                }`}
              >
                <Icon className={section.seg === "rewards" && !active ? "text-[#FFCC4F]" : undefined} />
                {t(section.i18nKey)}
                {section.seg === "orders" && !!summary?.progress && (
                  <span className="ml-auto text-[11px] px-2 py-0.5 rounded-full bg-mag/10 text-mag">
                    {summary.progress}
                  </span>
                )}
              </Link>
            );
          })}

          <div className="my-1 border-t border-ink/8" />

          <button
            type="button"
            onClick={logout}
            className="w-full flex items-center gap-3 px-4 py-[0.7rem] rounded-2xl text-sale hover:bg-sale/5"
          >
            <SignOutIcon />
            {t("signOut")}
          </button>
        </nav>
      </div>
    </aside>
  );
}
