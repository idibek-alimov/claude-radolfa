"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/features/auth";
import { useAdminShell } from "../model/AdminShellContext";
import { ADMIN_NAV_GROUPS } from "../model/navItems";
import { cn } from "@/shared/lib";
import type { AdminNavItem } from "../model/types";

function isActive(href: string, pathname: string) {
  if (href === "/manage") return pathname === "/manage";
  return pathname.startsWith(href);
}

function NavItem({ item, collapsed }: { item: AdminNavItem; collapsed: boolean }) {
  const pathname = usePathname();
  const active = isActive(item.href, pathname);

  return (
    // Wrapper for CSS tooltip — `group` scoped here
    <div className="relative group/nav">
      <Link
        href={item.href}
        className={cn(
          "relative flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition-all duration-150",
          "focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-amber-500/50",
          active
            ? "bg-amber-50 text-amber-700"
            : "text-zinc-600 hover:bg-zinc-100 hover:text-zinc-900",
          collapsed && "justify-center px-2"
        )}
      >
        {/* Active left border indicator */}
        {active && (
          <span className="absolute left-0 top-1/2 -translate-y-1/2 w-[2px] h-5 rounded-r-full bg-amber-500" />
        )}

        <item.icon
          className={cn(
            "shrink-0 transition-colors duration-150",
            collapsed ? "h-[18px] w-[18px]" : "h-4 w-4",
            active ? "text-amber-600" : "text-zinc-500 group-hover/nav:text-zinc-700"
          )}
        />

        {/* Label — always rendered, animated via max-width + opacity */}
        <span
          className={cn(
            "truncate font-medium tracking-[-0.01em] overflow-hidden whitespace-nowrap",
            "transition-[opacity,max-width] duration-200 ease-in-out",
            collapsed ? "max-w-0 opacity-0" : "max-w-[160px] opacity-100"
          )}
        >
          {item.label}
        </span>
      </Link>

      {/* CSS tooltip — only visible when collapsed, shown on hover */}
      {collapsed && (
        <div
          className={cn(
            "pointer-events-none absolute left-full top-1/2 -translate-y-1/2 ml-3 z-[60]",
            "opacity-0 group-hover/nav:opacity-100 transition-opacity duration-150",
            "bg-zinc-900 text-white border border-zinc-800",
            "text-[11px] font-semibold tracking-wide",
            "px-2.5 py-1.5 rounded-md whitespace-nowrap shadow-lg shadow-black/20"
          )}
        >
          {item.label}
        </div>
      )}
    </div>
  );
}

export function AdminSidebar() {
  const { collapsed, mobileOpen, closeMobile } = useAdminShell();
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  const initials = user?.phone ? user.phone.slice(-2) : "AD";
  const roleLabel = user?.role ?? "MANAGER";
  const roleColor =
    roleLabel === "ADMIN"
      ? "text-rose-600"
      : roleLabel === "MANAGER"
      ? "text-blue-600"
      : "text-zinc-500";

  return (
    <>
      {/* ── Mobile backdrop ─────────────────────────────────────── */}
      <div
        className={cn(
          "fixed inset-0 z-40 bg-black/40 backdrop-blur-[2px] lg:hidden",
          "transition-opacity duration-[250ms] ease-in-out",
          mobileOpen ? "opacity-100" : "opacity-0 pointer-events-none"
        )}
        onClick={closeMobile}
        aria-hidden="true"
      />

      {/* ── Sidebar ─────────────────────────────────────────────── */}
      <aside
        className={cn(
          "fixed left-0 top-14 z-50 flex flex-col",
          "h-[calc(100vh-3.5rem)]",
          "bg-white border-r border-zinc-200",
          // Mobile: fixed 240px drawer, slides in/out via transform
          "w-[240px]",
          "transition-transform duration-[250ms] ease-in-out",
          mobileOpen ? "translate-x-0" : "-translate-x-full",
          // Desktop: override transform, collapse via width
          "lg:translate-x-0 lg:transition-[width] lg:duration-200 lg:ease-in-out",
          collapsed ? "lg:w-[56px]" : "lg:w-[240px]"
        )}
      >
        {/* Subtle vertical gradient for depth */}
        <div className="pointer-events-none absolute inset-0 bg-gradient-to-b from-zinc-50/40 to-transparent" />

        {/* Nav content */}
        <nav className="relative flex-1 overflow-y-auto overflow-x-hidden py-4 scrollbar-hide">
          {ADMIN_NAV_GROUPS.map((group) => {
            const visibleItems = group.items.filter(
              (item) => !item.adminOnly || isAdmin
            );
            if (visibleItems.length === 0) return null;

            return (
              <div key={group.label} className="mb-5">
                {/* Group label — only when expanded (desktop) */}
                <p
                  className={cn(
                    "mb-1 px-3 text-[9px] font-semibold uppercase tracking-[0.2em] text-zinc-400 select-none",
                    "transition-[opacity,max-width] duration-200 ease-in-out overflow-hidden whitespace-nowrap",
                    collapsed ? "lg:opacity-0 lg:max-h-0 lg:mb-0" : "opacity-100"
                  )}
                >
                  {group.label}
                </p>

                {/* Divider when desktop-collapsed */}
                {group.label !== "Overview" && (
                  <div
                    className={cn(
                      "mx-auto border-t border-zinc-200 transition-all duration-200",
                      collapsed ? "lg:w-6 lg:mb-2 lg:opacity-100" : "w-0 opacity-0 mb-0"
                    )}
                  />
                )}

                <div className={cn("flex flex-col gap-0.5", collapsed ? "lg:px-1.5" : "px-2")}>
                  {visibleItems.map((item) => (
                    <NavItem key={item.href} item={item} collapsed={collapsed} />
                  ))}
                </div>
              </div>
            );
          })}
        </nav>

        {/* Bottom user section */}
        <div
          className={cn(
            "relative border-t border-zinc-200 p-3",
            collapsed ? "lg:flex lg:justify-center" : "flex items-center gap-3"
          )}
        >
          {/* Avatar */}
          <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-amber-500/20 ring-1 ring-amber-500/30">
            <span className="text-[11px] font-bold text-amber-600 tracking-wider">
              {initials}
            </span>
          </div>

          <div
            className={cn(
              "min-w-0 flex-1 overflow-hidden",
              "transition-[opacity,max-width] duration-200 ease-in-out",
              collapsed ? "lg:max-w-0 lg:opacity-0" : "max-w-[160px] opacity-100"
            )}
          >
            <p className="truncate text-xs font-medium text-zinc-900">
              {user?.phone ?? "Manager"}
            </p>
            <p className={cn("text-[10px] font-semibold uppercase tracking-wider", roleColor)}>
              {roleLabel}
            </p>
          </div>
        </div>
      </aside>
    </>
  );
}
