import { ScanBarcode, PackagePlus, LayoutGrid, Undo2, ScanLine, Forklift } from "lucide-react";
import type { LucideIcon } from "lucide-react";

export interface WarehouseNavItem {
  href: string;
  icon: LucideIcon;
  translationKey: string;
}

export const WAREHOUSE_NAV_ITEMS: WarehouseNavItem[] = [
  { href: "/warehouse/lookup",    icon: ScanBarcode,  translationKey: "nav.lookup"   },
  { href: "/warehouse/receipts",  icon: PackagePlus,  translationKey: "nav.receipts" },
  { href: "/warehouse/putaway",   icon: Forklift,     translationKey: "nav.putaway"  },
  { href: "/warehouse/structure", icon: LayoutGrid,   translationKey: "nav.structure" },
  { href: "/warehouse/returns",   icon: Undo2,        translationKey: "nav.returns"  },
  { href: "/warehouse/pick",      icon: ScanLine,     translationKey: "nav.pick"     },
];
