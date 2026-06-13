"use client";

import { useState } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { useAuth } from "@radolfa/shared/auth";
import { useLoyaltyTiers } from "@/entities/loyalty";
import { fetchCategoryTree } from "@/entities/product/api";
import { getCategoryTheme } from "@/entities/category";
import {
  Sheet,
  SheetTrigger,
  SheetContent,
  SheetClose,
} from "@radolfa/shared/ui/sheet";
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
import { ChevronRight, LogOut, MapPin, Phone } from "lucide-react";
import LanguageSwitcher from "./LanguageSwitcher";
import { toast } from "sonner";
import { useTranslations } from "next-intl";

/* ── UserAvatar (local copy for the drawer) ─────────────────── */
function UserAvatar({ name, phone }: { name?: string; phone: string }) {
  const initials = name
    ? name
        .split(" ")
        .map((n) => n[0])
        .join("")
        .toUpperCase()
        .slice(0, 2)
    : phone.slice(-2);

  return (
    <div className="h-10 w-10 rounded-full bg-gradient-to-br from-mag/20 to-mag/5 text-mag flex items-center justify-center font-semibold text-sm ring-2 ring-mag/30 ring-offset-1 shrink-0">
      {initials}
    </div>
  );
}

/* ── Logout dialog ──────────────────────────────────────────── */
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
          <AlertDialogTitle className="text-center">{t("logoutTitle")}</AlertDialogTitle>
          <AlertDialogDescription className="text-center">{t("logoutDescription")}</AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter className="sm:justify-center gap-3 mt-2">
          <AlertDialogCancel className="sm:min-w-[120px]">{t("logoutCancel")}</AlertDialogCancel>
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

/* ── MobileMenu ─────────────────────────────────────────────── */
export default function MobileMenu() {
  const { user, isAuthenticated, isLoading, logout } = useAuth();
  const t = useTranslations("navbar");
  const [logoutDialogOpen, setLogoutDialogOpen] = useState(false);

  const { data: tiers } = useLoyaltyTiers();
  const topTier = tiers?.find((ti) => ti.displayOrder === 1);

  const { data: categories } = useQuery({
    queryKey: ["categories", "tree"],
    queryFn: fetchCategoryTree,
    staleTime: 30 * 60 * 1000,
  });

  const handleLogout = async () => {
    setLogoutDialogOpen(false);
    await logout();
    toast.success(t("loggedOut"));
  };

  return (
    <>
      <Sheet>
        <SheetTrigger asChild>
          <button
            className="h-9 w-9 flex items-center justify-center"
            aria-label="Open menu"
          >
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" aria-hidden>
              <path d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>
        </SheetTrigger>

        <SheetContent side="left" className="w-[88vw] max-w-[320px] p-0 flex flex-col overflow-y-auto">

          {/* ── Header row ───────────────────────────────────── */}
          <div className="flex items-center justify-between pl-5 pr-12 py-4 border-b border-ink/8">
            <span className="text-xl font-extrabold text-ink tracking-tight">Radolfa</span>
            <LanguageSwitcher align="right" />
          </div>

          <div className="flex flex-col flex-1 px-4 py-3 gap-3">

            {/* ── Sign-in / User strip ─────────────────────── */}
            {isLoading ? (
              <div className="h-16 rounded-xl bg-muted animate-pulse" />
            ) : isAuthenticated && user ? (
              <SheetClose asChild>
                <Link
                  href="/profile"
                  className="flex items-center gap-3 p-3 rounded-xl bg-plum/60 hover:bg-plum/80 transition-colors"
                >
                  <UserAvatar name={user.name} phone={user.phone} />
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-bold text-ink truncate">
                      {user.name || user.phone}
                    </p>
                    <p className="text-xs text-ink/55 mt-0.5">View profile</p>
                  </div>
                  <ChevronRight className="h-4 w-4 text-ink/40 shrink-0" />
                </Link>
              </SheetClose>
            ) : (
              <SheetClose asChild>
                <Link
                  href="/login"
                  className="flex items-center gap-3 p-3 rounded-xl bg-plum/60 hover:bg-plum/80 transition-colors"
                >
                  {/* User icon */}
                  <div className="h-10 w-10 rounded-full bg-mag/10 flex items-center justify-center shrink-0">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#CB11AB" strokeWidth="2" aria-hidden>
                      <circle cx="12" cy="7" r="4" />
                      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                    </svg>
                  </div>
                  <div className="flex-1">
                    <p className="text-sm font-bold text-ink">Sign in or create account</p>
                    <p className="text-xs text-ink/55 mt-0.5">Track orders · view your tier</p>
                  </div>
                  <ChevronRight className="h-4 w-4 text-ink/40 shrink-0" />
                </Link>
              </SheetClose>
            )}

            {/* ── Loyalty strip ────────────────────────────── */}
            <SheetClose asChild>
              <Link
                href="/loyalty"
                className="flex items-center gap-3 p-3 rounded-xl bg-gradient-to-br from-mag to-maghi text-white hover:opacity-90 transition-opacity"
              >
                <div className="h-10 w-10 rounded-full bg-white/20 flex items-center justify-center shrink-0">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
                    <path d="M5 16l-3-8 5.5 4L12 4l4.5 8L22 8l-3 8H5z" />
                  </svg>
                </div>
                <div className="flex-1">
                  <p className="text-sm font-bold">Crown tier</p>
                  <p className="text-xs text-white/80 mt-0.5">
                    {topTier
                      ? `${topTier.discountPercentage}% off forever`
                      : "Exclusive discounts"}
                  </p>
                </div>
                <ChevronRight className="h-4 w-4 text-white/70 shrink-0" />
              </Link>
            </SheetClose>

            {/* ── Browse categories ─────────────────────────── */}
            <div>
              <p className="text-[11px] tracking-widest uppercase text-muted-foreground font-semibold px-1 mb-2">
                Browse categories
              </p>

              {/* Mega sale row */}
              <SheetClose asChild>
                <Link
                  href="/collections/on_sale"
                  className="flex items-center gap-3 py-2.5 px-2 rounded-lg hover:bg-plum/30 transition-colors"
                >
                  <div className="h-9 w-9 rounded-xl bg-sale flex items-center justify-center shrink-0">
                    <span className="text-lg">🔥</span>
                  </div>
                  <span className="flex-1 text-sm font-semibold text-sale">Mega Sale</span>
                  <span className="px-1.5 py-0.5 rounded bg-sale text-white text-[10px] font-bold">
                    −70%
                  </span>
                </Link>
              </SheetClose>
              <div className="h-px bg-ink/5 my-1" />

              {/* Dynamic root categories */}
              {categories?.map((cat, i) => {
                const theme = getCategoryTheme(cat.slug);
                return (
                  <div key={cat.id}>
                    <SheetClose asChild>
                      <Link
                        href={`/categories/${cat.slug}/products`}
                        className="flex items-center gap-3 py-2.5 px-2 rounded-lg hover:bg-plum/30 transition-colors"
                      >
                        <div
                          className={`h-9 w-9 rounded-xl bg-gradient-to-br ${theme.gradient} flex items-center justify-center shrink-0`}
                        >
                          <span className={`text-xs font-bold ${theme.text === "ink" ? "text-ink" : "text-white"}`}>
                            {cat.name.slice(0, 2).toUpperCase()}
                          </span>
                        </div>
                        <span className="flex-1 text-sm font-medium text-ink">
                          {cat.name}
                        </span>
                        <ChevronRight className="h-4 w-4 text-ink/30 shrink-0" />
                      </Link>
                    </SheetClose>
                    {i < (categories?.length ?? 0) - 1 && (
                      <div className="h-px bg-ink/5 my-1" />
                    )}
                  </div>
                );
              })}
            </div>

            {/* ── Utility links ─────────────────────────────── */}
            <div className="mt-1">
              <div className="h-px bg-ink/5 mb-3" />
              <SheetClose asChild>
                <Link
                  href="/orders"
                  className="flex items-center gap-3 py-2.5 px-2 rounded-lg hover:bg-muted transition-colors"
                >
                  <MapPin className="h-4 w-4 text-ink/50 shrink-0" />
                  <span className="text-sm text-ink/70">Track my order</span>
                </Link>
              </SheetClose>
              <div className="flex items-center gap-3 py-2.5 px-2 rounded-lg">
                <Phone className="h-4 w-4 text-ink/50 shrink-0" />
                <span className="text-sm text-ink/50">Contact support</span>
              </div>
            </div>

            {/* ── Logout ───────────────────────────────────── */}
            {isAuthenticated && user && (
              <div className="mt-auto pt-2 border-t border-ink/8">
                <button
                  onClick={() => setLogoutDialogOpen(true)}
                  className="flex items-center gap-2.5 text-sm text-destructive hover:text-destructive/80 py-3 px-3 rounded-xl hover:bg-destructive/10 w-full transition-colors"
                >
                  <LogOut className="h-4 w-4" />
                  {t("logout")}
                </button>
              </div>
            )}
          </div>

          {/* ── Footer ───────────────────────────────────────── */}
          <div className="border-t border-ink/8 px-5 py-3 flex items-center justify-center">
            <span className="text-[11px] text-ink/40">Radolfa © 2026</span>
          </div>
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
