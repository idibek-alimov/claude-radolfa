import {
  OverviewIcon,
  OrdersIcon,
  OrdersTabIcon,
  RewardsIcon,
  AddressesIcon,
  AddressesTabIcon,
  PaymentIcon,
  PaymentTabIcon,
  SettingsIcon,
} from "./icons";

/** Single source of truth for the six profile sections — drives the desktop
 *  sidebar, the mobile header/tab-strip, and the breadcrumb. */
export const PROFILE_SECTIONS = [
  {
    seg: "overview",
    href: "/profile",
    i18nKey: "sectionOverview",
    icon: OverviewIcon,
    tabIcon: OverviewIcon,
  },
  {
    seg: "orders",
    href: "/profile/orders",
    i18nKey: "sectionOrders",
    icon: OrdersIcon,
    tabIcon: OrdersTabIcon,
  },
  {
    seg: "rewards",
    href: "/profile/rewards",
    i18nKey: "sectionRewards",
    icon: RewardsIcon,
    tabIcon: RewardsIcon,
  },
  {
    seg: "addresses",
    href: "/profile/addresses",
    i18nKey: "sectionAddresses",
    icon: AddressesIcon,
    tabIcon: AddressesTabIcon,
  },
  {
    seg: "payment",
    href: "/profile/payment",
    i18nKey: "sectionPayment",
    icon: PaymentIcon,
    tabIcon: PaymentTabIcon,
  },
  {
    seg: "settings",
    href: "/profile/settings",
    i18nKey: "sectionSettings",
    icon: SettingsIcon,
    tabIcon: SettingsIcon,
  },
] as const;

export type ProfileSection = (typeof PROFILE_SECTIONS)[number];

/** Active match: Overview only matches the exact `/profile` root; the rest
 *  match their route prefix (mirrors `BottomNav`'s `isActive`). */
export function isSectionActive(section: ProfileSection, pathname: string): boolean {
  return section.seg === "overview"
    ? pathname === "/profile"
    : pathname.startsWith(section.href);
}

export function activeSection(pathname: string): ProfileSection {
  return (
    PROFILE_SECTIONS.find((s) => isSectionActive(s, pathname)) ?? PROFILE_SECTIONS[0]
  );
}
