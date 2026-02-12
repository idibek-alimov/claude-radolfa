"use client";

import Link from "next/link";
import { useAuth } from "@/features/auth";
import { SearchBar } from "@/features/search";
import {
  Menu,
  User,
  LogOut,
  ChevronDown,
  Settings,
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
import LanguageSwitcher from "./LanguageSwitcher";

/* ── User Avatar ───────────────────────────────────────────────── */
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
    <div className="h-8 w-8 rounded-full bg-primary/10 text-primary flex items-center justify-center text-xs font-semibold">
      {initials}
    </div>
  );
}

/* ── Desktop Auth Section ──────────────────────────────────────── */
function DesktopAuth() {
  const { user, isAuthenticated, isLoading, logout } = useAuth();

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
        <span className="hidden lg:inline">Sign in</span>
      </Link>
    );
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger className="flex items-center gap-1.5 rounded-lg hover:bg-accent/60 pl-1 pr-2.5 py-1 transition-colors outline-none">
        <UserAvatar name={user.name} phone={user.phone} />
        <ChevronDown className="h-3.5 w-3.5 text-muted-foreground" />
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-52 rounded-xl">
        <DropdownMenuLabel className="flex items-center gap-2">
          <span>{user.name || user.phone}</span>
          <span
            className={`px-1.5 py-0.5 text-[10px] rounded-full font-medium ${
              user.role === "MANAGER"
                ? "bg-purple-100 text-purple-700"
                : "bg-blue-100 text-blue-700"
            }`}
          >
            {user.role}
          </span>
        </DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem asChild>
          <Link href="/profile" className="cursor-pointer">
            <User className="mr-2 h-4 w-4" />
            My Profile
          </Link>
        </DropdownMenuItem>
        {user.role === "MANAGER" && (
          <DropdownMenuItem asChild>
            <Link href="/manage" className="cursor-pointer">
              <Settings className="mr-2 h-4 w-4" />
              Manage Products
            </Link>
          </DropdownMenuItem>
        )}
        <DropdownMenuSeparator />
        <DropdownMenuItem
          onClick={logout}
          className="text-destructive cursor-pointer"
        >
          <LogOut className="mr-2 h-4 w-4" />
          Logout
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

/* ── Mobile Sheet (Auth, Lang, Manage — NO search) ─────────────── */
function MobileMenu() {
  const { user, isAuthenticated, isLoading, logout } = useAuth();

  return (
    <Sheet>
      <SheetTrigger className="p-2 rounded-lg hover:bg-accent transition-colors md:hidden">
        <Menu className="h-5 w-5" />
        <span className="sr-only">Open menu</span>
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

        <nav className="flex flex-col gap-2 mt-8">
          {user?.role === "MANAGER" && (
            <SheetClose asChild>
              <Link
                href="/manage"
                className="flex items-center gap-2.5 text-sm font-medium text-purple-600 hover:text-purple-700 transition-colors py-2.5 px-2 rounded-lg hover:bg-purple-50"
              >
                <Settings className="h-4 w-4" />
                Manage Products
              </Link>
            </SheetClose>
          )}

          {/* Language */}
          <div className="flex items-center gap-2 py-3 px-2">
            <span className="text-xs text-muted-foreground">Language:</span>
            <LanguageSwitcher />
          </div>

          {/* Auth */}
          <div className="border-t pt-4 mt-2">
            {isLoading ? (
              <div className="h-10 w-full bg-muted animate-pulse rounded-lg" />
            ) : isAuthenticated && user ? (
              <>
                <div className="flex items-center gap-3 mb-4 px-2">
                  <UserAvatar name={user.name} phone={user.phone} />
                  <div>
                    <p className="text-sm font-medium">
                      {user.name || user.phone}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {user.role}
                    </p>
                  </div>
                </div>
                <SheetClose asChild>
                  <Link
                    href="/profile"
                    className="flex items-center gap-2 text-sm text-foreground hover:text-primary py-2.5 px-2 rounded-lg hover:bg-accent"
                  >
                    <User className="h-4 w-4" />
                    My Profile
                  </Link>
                </SheetClose>
                <button
                  onClick={logout}
                  className="flex items-center gap-2 text-sm text-destructive hover:text-destructive/80 py-2.5 px-2 rounded-lg hover:bg-red-50 w-full"
                >
                  <LogOut className="h-4 w-4" />
                  Logout
                </button>
              </>
            ) : (
              <SheetClose asChild>
                <Link
                  href="/login"
                  className="flex items-center gap-2 text-sm font-medium text-foreground hover:text-primary py-2.5 px-2 rounded-lg hover:bg-accent"
                >
                  <User className="h-4 w-4" />
                  Sign in
                </Link>
              </SheetClose>
            )}
          </div>
        </nav>
      </SheetContent>
    </Sheet>
  );
}

/* ── Main Navbar ───────────────────────────────────────────────── */
export default function Navbar() {
  return (
    <nav className="sticky top-0 z-40 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 border-b">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
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
    </nav>
  );
}
