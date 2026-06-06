"use client";

import { useState } from "react";
import Link from "next/link";
import { useAuth } from "@radolfa/shared/auth";
import { SearchBar } from "@/features/search";
import { useQuery } from "@tanstack/react-query";
import { getMyOrders } from "@/features/profile/api";
import {
  User,
  LogOut,
  ChevronDown,
  Settings,
  Star,
} from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuLabel,
} from "@radolfa/shared/ui/dropdown-menu";
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogFooter,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogAction,
  AlertDialogCancel,
} from "@radolfa/shared/ui/alert-dialog";
import { MegaMenu } from "@/widgets/MegaMenu";
import { CartIconButton } from "@/features/cart";
import MobileMenu from "./MobileMenu";
import LanguageSwitcher from "./LanguageSwitcher";
import { useTranslations } from "next-intl";
import { toast } from "sonner";
import { opsUrl } from "@radolfa/shared/lib";

/* ── Role-based Avatar ─────────────────────────────────────────── */
function UserAvatar({
  name,
  phone,
  role,
  size = "sm",
}: {
  name?: string;
  phone: string;
  role?: string;
  size?: "sm" | "lg";
}) {
  const initials = name
    ? name
        .split(" ")
        .map((n) => n[0])
        .join("")
        .toUpperCase()
        .slice(0, 2)
    : phone.slice(-2);

  const ringColor =
    role === "MANAGER" || role === "ADMIN"
      ? "ring-purple-400"
      : "ring-primary/30";

  const sizeClasses =
    size === "lg"
      ? "h-10 w-10 text-sm ring-[2.5px]"
      : "h-8 w-8 text-xs ring-2";

  return (
    <div
      className={`${sizeClasses} rounded-full bg-gradient-to-br from-primary/20 to-primary/5 text-primary flex items-center justify-center font-semibold ${ringColor} ring-offset-1 ring-offset-background`}
    >
      {initials}
    </div>
  );
}

/* ── Logout Confirmation Dialog ────────────────────────────────── */
function LogoutDialog({
  open,
  onOpenChange,
  onConfirm,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirm: () => void;
}) {
  const t = useTranslations("navbar");

  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent className="sm:max-w-[400px]">
        <AlertDialogHeader>
          <div className="mx-auto mb-2 flex h-12 w-12 items-center justify-center rounded-full bg-destructive/10">
            <LogOut className="h-6 w-6 text-destructive" />
          </div>
          <AlertDialogTitle className="text-center">
            {t("logoutTitle")}
          </AlertDialogTitle>
          <AlertDialogDescription className="text-center">
            {t("logoutDescription")}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter className="sm:justify-center gap-3 mt-2">
          <AlertDialogCancel className="sm:min-w-[120px]">
            {t("logoutCancel")}
          </AlertDialogCancel>
          <AlertDialogAction
            onClick={onConfirm}
            className="bg-destructive text-destructive-foreground hover:bg-destructive/90 sm:min-w-[120px]"
          >
            {t("logoutConfirm")}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}

/* ── Desktop Auth Section ──────────────────────────────────────── */
function DesktopAuth() {
  const { user, isAuthenticated, isLoading, logout } = useAuth();
  const t = useTranslations("navbar");
  const tp = useTranslations("profile");
  const [logoutDialogOpen, setLogoutDialogOpen] = useState(false);

  const { data: ordersPage } = useQuery({
    queryKey: ["my-orders", 1, 1],
    queryFn: () => getMyOrders(1, 1),
    enabled: isAuthenticated,
    staleTime: 5 * 60 * 1000,
  });

  const latestOrder = ordersPage?.content[0];

  const handleLogout = async () => {
    setLogoutDialogOpen(false);
    await logout();
    toast.success(t("loggedOut"));
  };

  if (isLoading) {
    return <div className="h-8 w-8 rounded-full bg-muted animate-pulse" />;
  }

  if (!isAuthenticated || !user) {
    return (
      <Link
        href="/login"
        className="px-3 py-2 rounded hover:bg-plum/40 inline-flex items-center gap-2 transition-colors"
      >
        <svg
          width="22"
          height="22"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          aria-hidden
        >
          <circle cx="12" cy="7" r="4" />
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
        </svg>
        <span className="text-[13px] font-semibold">{t("signIn")}</span>
      </Link>
    );
  }

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger className="px-3 py-2 rounded hover:bg-plum/40 inline-flex items-center gap-2 outline-none transition-colors">
          <UserAvatar name={user.name} phone={user.phone} role={user.role} />
          <ChevronDown className="h-3.5 w-3.5 text-muted-foreground" />
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="w-64 rounded-xl">
          {/* User info header */}
          <DropdownMenuLabel className="py-3">
            <div className="flex items-center gap-2">
              <span className="font-medium">{user.name || user.phone}</span>
              <span
                className={`px-1.5 py-0.5 text-[10px] rounded-full font-medium ${
                  user.role === "MANAGER"
                    ? "bg-purple-100 text-purple-700"
                    : "bg-blue-100 text-blue-700"
                }`}
              >
                {user.role}
              </span>
            </div>
            {(user.loyalty?.tier || (user.loyalty?.points ?? 0) > 0) && (
              <div className="flex items-center gap-1.5 mt-1.5">
                <Star className="h-3 w-3 text-amber-500 fill-amber-500" />
                <span className="text-xs text-muted-foreground">
                  {tp("points", { count: user.loyalty?.points ?? 0 })}
                </span>
              </div>
            )}
          </DropdownMenuLabel>

          {/* Latest order preview */}
          {latestOrder && (
            <>
              <DropdownMenuSeparator />
              <DropdownMenuItem asChild className="py-2.5 cursor-pointer">
                <Link
                  href="/profile?tab=orders"
                  className="flex items-center gap-2"
                >
                  <svg
                    width="14"
                    height="14"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    className="text-muted-foreground shrink-0"
                    aria-hidden
                  >
                    <path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4Z" />
                  </svg>
                  <span className="text-xs text-muted-foreground truncate">
                    {tp("recentOrder", {
                      id: latestOrder.id,
                      status: latestOrder.status,
                    })}
                  </span>
                </Link>
              </DropdownMenuItem>
            </>
          )}

          <DropdownMenuSeparator />
          {user.loyalty?.tier && (
            <DropdownMenuItem asChild className="py-2 cursor-pointer">
              <Link
                href="/profile?tab=loyalty"
                className="flex items-center gap-2"
              >
                <Star className="h-3.5 w-3.5 text-amber-500 fill-amber-500 shrink-0" />
                <span className="text-xs">
                  <span className="font-medium text-foreground">
                    {user.loyalty.tier.name}
                  </span>
                  <span className="text-muted-foreground">
                    {" "}
                    · {user.loyalty.tier.discountPercentage}% {tp("discount")}
                  </span>
                </span>
              </Link>
            </DropdownMenuItem>
          )}
          <DropdownMenuItem asChild className="py-3 cursor-pointer">
            <Link href="/profile">
              <User className="mr-2 h-4 w-4" />
              {t("myProfile")}
            </Link>
          </DropdownMenuItem>
          {(user.role === "MANAGER" || user.role === "ADMIN") && (
            <DropdownMenuItem asChild className="py-3 cursor-pointer">
              {/* Cross-app link: opsUrl() swaps port in dev, relative behind nginx */}
              <a href={opsUrl("/ops/manage")}>
                <Settings className="mr-2 h-4 w-4" />
                {t("management")}
              </a>
            </DropdownMenuItem>
          )}
          <DropdownMenuSeparator className="my-1" />
          <div className="px-1 pb-1 pt-1">
            <DropdownMenuItem
              onClick={() => setLogoutDialogOpen(true)}
              className="py-3 cursor-pointer text-destructive focus:text-destructive focus:bg-destructive/10 rounded-lg"
            >
              <LogOut className="mr-2 h-4 w-4" />
              {t("logout")}
            </DropdownMenuItem>
          </div>
        </DropdownMenuContent>
      </DropdownMenu>

      <LogoutDialog
        open={logoutDialogOpen}
        onOpenChange={setLogoutDialogOpen}
        onConfirm={handleLogout}
      />
    </>
  );
}

/* ── Main Navbar ───────────────────────────────────────────────── */
export default function Navbar() {
  return (
    <header className="bg-white sticky top-0 z-30 border-b border-ink/8">
      {/* ─── Desktop ────────────────────────────────────────── */}
      <div className="hidden md:block">
        <div className="max-w-[1440px] mx-auto px-6 py-4 flex items-center gap-5">
          {/* Wordmark */}
          <Link href="/" className="shrink-0">
            <span className="text-2xl font-extrabold tracking-tight text-ink hover:text-mag transition-colors">
              Radolfa
            </span>
          </Link>

          {/* Search */}
          <div className="flex-1 relative max-w-3xl mx-auto">
            <SearchBar compact />
          </div>

          {/* Right utilities */}
          <div className="flex items-center gap-1 text-[12px] shrink-0">
            <LanguageSwitcher />
            <DesktopAuth />
            <CartIconButton />
          </div>
        </div>

        {/* Flat categories ribbon (inside <header> per reference) */}
        <MegaMenu />
      </div>

      {/* ─── Mobile ─────────────────────────────────────────── */}
      <div className="md:hidden px-4 py-3 flex items-center gap-2.5">
        {/* Hamburger — opens left drawer */}
        <MobileMenu />

        {/* Wordmark */}
        <Link href="/">
          <span className="text-xl font-extrabold tracking-tight text-ink">Radolfa</span>
        </Link>

        {/* Search pill */}
        <div className="flex-1 relative ml-1">
          <SearchBar />
        </div>

        {/* Bag */}
        <CartIconButton />
      </div>
    </header>
  );
}
