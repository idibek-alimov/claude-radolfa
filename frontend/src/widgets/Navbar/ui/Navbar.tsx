"use client";

import Link from "next/link";
import { useAuth } from "@/features/auth";
import { Package, Menu, User, LogOut, ChevronDown, Settings } from "lucide-react";
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

const NAV_LINKS = [
  { label: "Home", href: "/" },
  { label: "Products", href: "/products" },
];

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

export default function Navbar() {
  const { user, isAuthenticated, isLoading, logout } = useAuth();

  return (
    <nav className="sticky top-0 z-40 bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60 border-b">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Brand */}
          <Link href="/" className="flex items-center gap-2">
            <Package className="h-6 w-6 text-primary" />
            <span className="text-xl font-bold text-foreground">Radolfa</span>
          </Link>

          {/* Desktop navigation */}
          <ul className="hidden md:flex items-center gap-6">
            {NAV_LINKS.map((link) => (
              <li key={link.href}>
                <Link
                  href={link.href}
                  className="text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
                >
                  {link.label}
                </Link>
              </li>
            ))}

            {user?.role === "MANAGER" && (
              <li>
                <Link
                  href="/manage"
                  className="text-sm font-medium text-purple-600 hover:text-purple-700 transition-colors"
                >
                  Manage
                </Link>
              </li>
            )}

            {/* Auth section */}
            {isLoading ? (
              <li className="text-sm text-muted-foreground">Loading...</li>
            ) : isAuthenticated && user ? (
              <li>
                <DropdownMenu>
                  <DropdownMenuTrigger className="flex items-center gap-2 text-sm font-medium text-foreground hover:text-foreground/80 transition-colors outline-none">
                    <UserAvatar name={user.name} phone={user.phone} />
                    <span className="hidden lg:inline">
                      {user.name || user.phone}
                    </span>
                    <span
                      className={`px-2 py-0.5 text-xs rounded-full ${
                        user.role === "MANAGER"
                          ? "bg-purple-100 text-purple-700"
                          : "bg-blue-100 text-blue-700"
                      }`}
                    >
                      {user.role}
                    </span>
                    <ChevronDown className="h-4 w-4 text-muted-foreground" />
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end" className="w-48">
                    <DropdownMenuLabel>
                      {user.name || user.phone}
                    </DropdownMenuLabel>
                    <DropdownMenuSeparator />
                    <DropdownMenuItem asChild>
                      <Link href="/profile" className="cursor-pointer">
                        <User className="mr-2 h-4 w-4" />
                        My Profile
                      </Link>
                    </DropdownMenuItem>
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
              </li>
            ) : (
              <li>
                <Link
                  href="/login"
                  className="inline-flex items-center gap-1.5 text-sm font-medium text-primary hover:text-primary/80 transition-colors"
                >
                  <User className="h-4 w-4" />
                  Login
                </Link>
              </li>
            )}
          </ul>

          {/* Mobile hamburger */}
          <div className="md:hidden">
            <Sheet>
              <SheetTrigger className="p-2 rounded-md hover:bg-accent transition-colors">
                <Menu className="h-5 w-5" />
                <span className="sr-only">Open menu</span>
              </SheetTrigger>
              <SheetContent side="right">
                <SheetHeader>
                  <SheetTitle>
                    <Link href="/" className="flex items-center gap-2">
                      <Package className="h-5 w-5 text-primary" />
                      Radolfa
                    </Link>
                  </SheetTitle>
                </SheetHeader>
                <nav className="flex flex-col gap-4 mt-8">
                  {NAV_LINKS.map((link) => (
                    <SheetClose key={link.href} asChild>
                      <Link
                        href={link.href}
                        className="text-base font-medium text-foreground hover:text-primary transition-colors py-2"
                      >
                        {link.label}
                      </Link>
                    </SheetClose>
                  ))}

                  {user?.role === "MANAGER" && (
                    <SheetClose asChild>
                      <Link
                        href="/manage"
                        className="text-base font-medium text-purple-600 hover:text-purple-700 transition-colors py-2 flex items-center gap-2"
                      >
                        <Settings className="h-4 w-4" />
                        Manage Products
                      </Link>
                    </SheetClose>
                  )}

                  <div className="border-t pt-4 mt-2">
                    {isAuthenticated && user ? (
                      <>
                        <div className="flex items-center gap-3 mb-4">
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
                            className="flex items-center gap-2 text-sm text-foreground hover:text-primary py-2"
                          >
                            <User className="h-4 w-4" />
                            My Profile
                          </Link>
                        </SheetClose>
                        <button
                          onClick={logout}
                          className="flex items-center gap-2 text-sm text-destructive hover:text-destructive/80 py-2 w-full"
                        >
                          <LogOut className="h-4 w-4" />
                          Logout
                        </button>
                      </>
                    ) : (
                      <SheetClose asChild>
                        <Link
                          href="/login"
                          className="flex items-center gap-2 text-sm font-medium text-primary py-2"
                        >
                          <User className="h-4 w-4" />
                          Login
                        </Link>
                      </SheetClose>
                    )}
                  </div>
                </nav>
              </SheetContent>
            </Sheet>
          </div>
        </div>
      </div>
    </nav>
  );
}
