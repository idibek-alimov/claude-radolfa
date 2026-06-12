import { LayoutDashboard, Package, ShoppingBag } from "lucide-react";
import type { LucideIcon } from "lucide-react";

export interface SellerNavItem {
  href: string;
  icon: LucideIcon;
  translationKey: string;
}

export const SELLER_NAV_ITEMS: SellerNavItem[] = [
  { href: "/seller",          icon: LayoutDashboard, translationKey: "nav.dashboard" },
  { href: "/seller/products", icon: Package,         translationKey: "nav.products"  },
  { href: "/seller/orders",   icon: ShoppingBag,     translationKey: "nav.orders"    },
];
