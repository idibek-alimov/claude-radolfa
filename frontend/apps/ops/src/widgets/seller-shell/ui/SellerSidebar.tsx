"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useTranslations } from "next-intl";
import { LogOut } from "lucide-react";
import { useAuth } from "@radolfa/shared/auth";
import { SELLER_NAV_ITEMS } from "../model/navItems";
import { cn } from "@radolfa/shared/lib";

function isActive(href: string, pathname: string) {
  return pathname.startsWith(href);
}

export function SellerSidebar() {
  const pathname = usePathname();
  const t = useTranslations("seller");
  const { user, logout } = useAuth();

  const initials = user?.phone ? user.phone.slice(-2) : "SE";

  return (
    <aside className="w-[240px] shrink-0 h-screen bg-white border-r border-zinc-200 flex flex-col">
      {/* Subtle gradient for depth */}
      <div className="pointer-events-none absolute inset-y-0 left-0 w-[240px] bg-gradient-to-b from-zinc-50/40 to-transparent" />

      {/* Brand header */}
      <div className="relative flex items-center gap-2.5 px-4 h-14 border-b border-zinc-200 shrink-0">
        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-primary/15 ring-1 ring-primary/30">
          <span className="text-[10px] font-bold text-primary">SE</span>
        </div>
        <span className="text-sm font-semibold text-zinc-900 tracking-tight">
          {t("title")}
        </span>
      </div>

      {/* Nav links */}
      <nav className="relative flex-1 overflow-y-auto py-4 px-2 space-y-0.5">
        {SELLER_NAV_ITEMS.map((item) => {
          const active = isActive(item.href, pathname);
          return (
            <div key={item.href} className="relative">
              <Link
                href={item.href}
                className={cn(
                  "relative flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition-all duration-150",
                  "focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-primary/50",
                  active
                    ? "bg-primary/5 text-primary"
                    : "text-zinc-600 hover:bg-zinc-100 hover:text-zinc-900"
                )}
              >
                {active && (
                  <span className="absolute left-0 top-1/2 -translate-y-1/2 w-[2px] h-5 rounded-r-full bg-primary" />
                )}
                <item.icon
                  className={cn(
                    "h-4 w-4 shrink-0 transition-colors duration-150",
                    active ? "text-primary" : "text-zinc-500"
                  )}
                />
                <span className="truncate font-medium tracking-[-0.01em]">
                  {t(item.translationKey as Parameters<typeof t>[0])}
                </span>
              </Link>
            </div>
          );
        })}
      </nav>

      {/* User footer */}
      <div className="relative border-t border-zinc-200 p-3 flex items-center gap-3">
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary/10 ring-1 ring-primary/20">
          <span className="text-[11px] font-bold text-primary tracking-wider">{initials}</span>
        </div>
        <div className="min-w-0 flex-1">
          <p className="truncate text-xs font-medium text-zinc-900">{user?.phone ?? "Seller"}</p>
          <p className="text-[10px] font-semibold uppercase tracking-wider text-zinc-400">
            {user?.role ?? "SELLER"}
          </p>
        </div>
        <button
          onClick={() => logout()}
          title={t("common.logout")}
          className="shrink-0 p-1.5 rounded-md text-zinc-400 hover:text-rose-600 hover:bg-rose-50 transition-colors"
        >
          <LogOut className="h-3.5 w-3.5" />
        </button>
      </div>
    </aside>
  );
}
