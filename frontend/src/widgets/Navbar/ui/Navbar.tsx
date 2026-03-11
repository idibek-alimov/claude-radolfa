"use client";

import { useState } from "react";
import Link from "next/link";
import { useAuth } from "@/features/auth";
import { SearchBar } from "@/features/search";
import { useQuery } from "@tanstack/react-query";
import { getMyOrders } from "@/features/profile/api";
import {
  Menu,
  User,
  LogOut,
  ChevronDown,
  Settings,
  Star,
  ShoppingBag,
} from "lucide-react";
import {
  Sheet,
  SheetTrigger,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetClose,
} from "@/shared/ui/sheet";
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuLabel,
} from "@/shared/ui/dropdown-menu";
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogFooter,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogAction,
  AlertDialogCancel,
} from "@/shared/ui/alert-dialog";
import { MegaMenu, MegaMenuMobile } from "@/widgets/MegaMenu";
import LanguageSwitcher from "./LanguageSwitcher";
import { useTranslations } from "next-intl";
import { toast } from "sonner";

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
    role === "MANAGER" || role === "SYSTEM"
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

  const { data: orders } = useQuery({
    queryKey: ["my-orders"],
    queryFn: getMyOrders,
    enabled: isAuthenticated,
    staleTime: 5 * 60 * 1000,
  });

  const latestOrder = orders?.[0];

  const handleLogout = async () => {
    setLogoutDialogOpen(false);
    await logout();
    toast.success(t("loggedOut"));
  };

  if (isLoading) {
    return (
      <div className="h-8 w-8 rounded-full bg-muted animate-pulse" />
    );
  }

  if (!isAuthenticated || !user) {
    return (
      <Link
        href="/login"
        className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium text-foreground hover:bg-accent transition-colors"
      >
        <User className="h-4 w-4" />
        <span className="hidden lg:inline">{t("signIn")}</span>
      </Link>
    );
  }

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger className="flex items-center gap-1.5 rounded-lg hover:bg-accent/60 pl-1 pr-2.5 py-1 transition-colors outline-none">
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
            {/* Loyalty points preview */}
            {user.loyaltyPoints > 0 && (
              <div className="flex items-center gap-1.5 mt-1.5">
                <Star className="h-3 w-3 text-amber-500 fill-amber-500" />
                <span className="text-xs text-muted-foreground">
                  {tp("points", { count: user.loyaltyPoints })}
                </span>
              </div>
            )}
          </DropdownMenuLabel>

          {/* Latest order preview */}
          {latestOrder && (
            <>
              <DropdownMenuSeparator />
              <DropdownMenuItem asChild className="py-2.5 cursor-pointer">
                <Link href="/profile?tab=orders" className="flex items-center gap-2">
                  <ShoppingBag className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
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
          <DropdownMenuItem asChild className="py-3 cursor-pointer">
            <Link href="/profile">
              <User className="mr-2 h-4 w-4" />
              {t("myProfile")}
            </Link>
          </DropdownMenuItem>
          {(user.role === "MANAGER" || user.role === "SYSTEM") && (
            <DropdownMenuItem asChild className="py-3 cursor-pointer">
              <Link href="/manage">
                <Settings className="mr-2 h-4 w-4" />
                {t("management")}
              </Link>
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

/* ── Mobile Sheet (Auth, Lang, Manage — NO search) ─────────────── */
function MobileMenu() {
  const { user, isAuthenticated, isLoading, logout } = useAuth();
  const t = useTranslations("navbar");
  const tp = useTranslations("profile");
  const [logoutDialogOpen, setLogoutDialogOpen] = useState(false);

  const handleLogout = async () => {
    setLogoutDialogOpen(false);
    await logout();
    toast.success(t("loggedOut"));
  };

  return (
    <>
      <Sheet>
        <SheetTrigger className="p-2 rounded-lg hover:bg-accent transition-colors md:hidden">
          <Menu className="h-5 w-5" />
          <span className="sr-only">{t("openMenu")}</span>
        </SheetTrigger>
        <SheetContent side="right" className="w-80">
          <SheetHeader>
            <SheetTitle>
              <Link href="/" className="flex items-center gap-2.5">
                <span className="text-xl font-bold tracking-tight text-foreground">
                  Radolfa
                </span>
              </Link>
            </SheetTitle>
          </SheetHeader>

          {/* Auth section — at the top for better UX */}
          <div className="mt-6 mb-4">
            {isLoading ? (
              <div className="h-14 w-full bg-muted animate-pulse rounded-xl" />
            ) : isAuthenticated && user ? (
              <SheetClose asChild>
                <Link
                  href="/profile"
                  className="flex items-center gap-3 p-3 rounded-xl bg-accent/50 hover:bg-accent transition-colors"
                >
                  <UserAvatar name={user.name} phone={user.phone} role={user.role} size="lg" />
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium truncate">
                      {user.name || user.phone}
                    </p>
                    <div className="flex items-center gap-2 mt-0.5">
                      <span className="text-xs text-muted-foreground">
                        {tp("viewProfile")}
                      </span>
                      {user.loyaltyPoints > 0 && (
                        <span className="flex items-center gap-0.5 text-xs text-amber-600">
                          <Star className="h-2.5 w-2.5 fill-amber-500" />
                          {user.loyaltyPoints}
                        </span>
                      )}
                    </div>
                  </div>
                  <span
                    className={`px-1.5 py-0.5 text-[10px] rounded-full font-medium shrink-0 ${
                      user.role === "MANAGER"
                        ? "bg-purple-100 text-purple-700"
                        : "bg-blue-100 text-blue-700"
                    }`}
                  >
                    {user.role}
                  </span>
                </Link>
              </SheetClose>
            ) : (
              <SheetClose asChild>
                <Link
                  href="/login"
                  className="flex items-center gap-2 text-sm font-medium text-foreground hover:text-primary py-3 px-3 rounded-xl hover:bg-accent transition-colors"
                >
                  <User className="h-4 w-4" />
                  {t("signIn")}
                </Link>
              </SheetClose>
            )}
          </div>

          <nav className="flex flex-col gap-2">
            {/* Categories accordion */}
            <MegaMenuMobile />

            {(user?.role === "MANAGER" || user?.role === "SYSTEM") && (
              <SheetClose asChild>
                <Link
                  href="/manage"
                  className="flex items-center gap-2.5 text-sm font-medium text-purple-600 hover:text-purple-700 transition-colors py-3 px-2 rounded-lg hover:bg-purple-50"
                >
                  <Settings className="h-4 w-4" />
                  {t("management")}
                </Link>
              </SheetClose>
            )}

            {/* Language */}
            <div className="flex items-center gap-2 py-3 px-2">
              <span className="text-xs text-muted-foreground">{t("language")}</span>
              <LanguageSwitcher />
            </div>

            {/* Logout — isolated at bottom with extra spacing */}
            {isAuthenticated && user && (
              <div className="border-t mt-4 pt-4">
                <button
                  onClick={() => setLogoutDialogOpen(true)}
                  className="flex items-center gap-2.5 text-sm text-destructive hover:text-destructive/80 py-3 px-3 rounded-xl hover:bg-destructive/10 w-full transition-colors"
                >
                  <LogOut className="h-4 w-4" />
                  {t("logout")}
                </button>
              </div>
            )}
          </nav>
        </SheetContent>
      </Sheet>

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
    <nav className="sticky top-0 z-40 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 border-b">
      <div className="max-w-[1600px] mx-auto px-4 sm:px-6 lg:px-8">
        {/* ─── Desktop: single row ─────────────────────────────── */}
        <div className="hidden md:flex items-center h-16 gap-6">
          {/* Left — Brand */}
          <Link
            href="/"
            className="flex items-center shrink-0 group"
          >
            <span className="text-3xl font-extrabold tracking-tight text-foreground group-hover:text-primary transition-colors duration-300">
              Radolfa
            </span>
          </Link>

          {/* Center — Search */}
          <div className="flex-1 flex justify-center px-6">
            <SearchBar compact />
          </div>

          {/* Right — Utilities */}
          <div className="flex items-center gap-2 shrink-0">
            <LanguageSwitcher />
            <DesktopAuth />
          </div>
        </div>

        {/* ─── Mobile: two rows ────────────────────────────────── */}
        <div className="md:hidden">
          {/* Row 1: Brand + Utilities */}
          <div className="flex items-center justify-between h-14">
            <Link
              href="/"
              className="flex items-center group"
            >
              <span className="text-2xl font-extrabold tracking-tight text-foreground">
                Radolfa
              </span>
            </Link>

            <div className="flex items-center gap-1">
              <LanguageSwitcher />
              <MobileMenu />
            </div>
          </div>

          {/* Row 2: Search bar — always visible */}
          <div className="pb-3">
            <SearchBar />
          </div>
        </div>
      </div>

      {/* Desktop: MegaMenu category bar */}
      <MegaMenu />
    </nav>
  );
}
