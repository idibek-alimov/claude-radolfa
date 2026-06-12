"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { ChevronRight } from "lucide-react";
import { ADMIN_NAV_GROUPS } from "../model/navItems";

const SEGMENT_LABELS: Record<string, string> = {
  manage: "Admin",
  products: "Products",
  users: "Users",
  categories: "Categories",
  colors: "Colors",
  tags: "Tags",
  discounts: "Discounts",
  tiers: "Tiers",
  reviews: "Reviews",
  qa: "Q&A",
  create: "Create",
  edit: "Edit",
};

function resolveLabel(href: string): string | undefined {
  for (const group of ADMIN_NAV_GROUPS) {
    for (const item of group.items) {
      if (item.href === href) return item.label;
    }
  }
  return undefined;
}

export function AdminBreadcrumb() {
  const pathname = usePathname();

  const segments = pathname.split("/").filter(Boolean);

  // Build parallel arrays: labels and their full paths
  const crumbs: { label: string; path: string }[] = [];
  let currentPath = "";

  for (const seg of segments) {
    currentPath += `/${seg}`;
    const navLabel = resolveLabel(currentPath);
    const fallback = SEGMENT_LABELS[seg] ?? (seg.length > 12 ? seg.slice(0, 10) + "…" : seg);
    crumbs.push({ label: navLabel ?? fallback, path: currentPath });
  }

  if (crumbs.length === 0) return null;

  return (
    <nav className="flex items-center gap-1 text-sm" aria-label="Breadcrumb">
      {crumbs.map(({ label, path }, idx) => {
        const isLast = idx === crumbs.length - 1;
        return (
          <span key={path} className="flex items-center gap-1">
            {idx > 0 && (
              <ChevronRight className="h-3.5 w-3.5 text-zinc-200 shrink-0" />
            )}
            {isLast ? (
              <span className="font-medium text-foreground">{label}</span>
            ) : (
              <Link
                href={path}
                className="text-muted-foreground transition-colors hover:text-zinc-700"
              >
                {label}
              </Link>
            )}
          </span>
        );
      })}
    </nav>
  );
}
