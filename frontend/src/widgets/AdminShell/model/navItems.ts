import {
  LayoutDashboard,
  Package,
  Folder,
  Palette,
  Tag,
  Percent,
  Award,
  Users,
  Star,
  HelpCircle,
} from "lucide-react";
import type { AdminNavGroup } from "./types";

export const ADMIN_NAV_GROUPS: AdminNavGroup[] = [
  {
    label: "Overview",
    items: [
      { href: "/manage", label: "Dashboard", icon: LayoutDashboard },
    ],
  },
  {
    label: "Catalog",
    items: [
      { href: "/manage/products", label: "Products", icon: Package },
      { href: "/manage/categories", label: "Categories", icon: Folder },
      { href: "/manage/colors", label: "Colors", icon: Palette },
      { href: "/manage/tags", label: "Tags", icon: Tag, adminOnly: true },
    ],
  },
  {
    label: "Commerce",
    items: [
      { href: "/manage/discounts", label: "Discounts", icon: Percent },
    ],
  },
  {
    label: "Loyalty",
    items: [
      { href: "/manage/tiers", label: "Tiers", icon: Award },
    ],
  },
  {
    label: "People",
    items: [
      { href: "/manage/users", label: "Users", icon: Users },
    ],
  },
  {
    label: "Moderation",
    items: [
      { href: "/manage/reviews", label: "Reviews", icon: Star },
      { href: "/manage/qa", label: "Q&A", icon: HelpCircle, adminOnly: true },
    ],
  },
];
