import type { LucideIcon } from "lucide-react";

export interface AdminNavItem {
  href: string;
  label: string;
  icon: LucideIcon;
  adminOnly?: boolean;
}

export interface AdminNavGroup {
  label: string;
  items: AdminNavItem[];
}
