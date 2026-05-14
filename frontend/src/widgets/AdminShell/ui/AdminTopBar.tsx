"use client";

import Link from "next/link";
import { Menu, ArrowLeft, LogOut } from "lucide-react";
import { useAuth } from "@/features/auth";
import { useAdminShell } from "../model/AdminShellContext";
import { AdminBreadcrumb } from "./AdminBreadcrumb";
import { Button } from "@/shared/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/shared/ui/dropdown-menu";
import { Badge } from "@/shared/ui/badge";
import { cn } from "@/shared/lib";

const ROLE_BADGE: Record<string, string> = {
  ADMIN: "bg-rose-500/10 text-rose-400 border-rose-500/20",
  MANAGER: "bg-blue-500/10 text-blue-500 border-blue-500/20",
  USER: "bg-zinc-500/10 text-zinc-500 border-zinc-500/20",
};

export function AdminTopBar() {
  const { toggle, openMobile } = useAdminShell();
  const { user, logout } = useAuth();

  const initials = user?.phone ? user.phone.slice(-2) : "AD";
  const role = user?.role ?? "MANAGER";

  function handleMenuClick() {
    if (typeof window !== "undefined" && window.innerWidth < 1024) {
      openMobile();
    } else {
      toggle();
    }
  }

  return (
    <header className="fixed top-0 left-0 right-0 z-40 flex h-14 items-center gap-4 border-b border-zinc-100 bg-white/95 px-4 backdrop-blur-sm">
      {/* Hamburger */}
      <Button
        variant="ghost"
        size="icon"
        onClick={handleMenuClick}
        className="h-8 w-8 shrink-0 text-zinc-500 hover:text-zinc-900 hover:bg-zinc-100"
        aria-label="Toggle sidebar"
      >
        <Menu className="h-4 w-4" />
      </Button>

      {/* Wordmark */}
      <div className="flex items-center gap-2 shrink-0">
        <span className="text-sm font-semibold tracking-tight text-zinc-900">
          Radolfa
        </span>
        <span className="hidden sm:inline-flex items-center rounded-full border border-zinc-200 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-widest text-zinc-400">
          Admin
        </span>
      </div>

      {/* Divider */}
      <div className="hidden sm:block h-4 w-px bg-zinc-200 shrink-0" />

      {/* Breadcrumb */}
      <div className="hidden sm:flex flex-1 min-w-0">
        <AdminBreadcrumb />
      </div>

      <div className="ml-auto flex items-center gap-2">
        {/* Back to store */}
        <Link
          href="/"
          className="hidden sm:flex items-center gap-1.5 rounded-md px-2.5 py-1.5 text-xs font-medium text-zinc-500 transition-colors hover:bg-zinc-100 hover:text-zinc-900"
        >
          <ArrowLeft className="h-3 w-3" />
          Store
        </Link>

        {/* User avatar dropdown */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <button
              className={cn(
                "flex h-8 w-8 items-center justify-center rounded-full",
                "bg-zinc-900 ring-2 ring-zinc-900/10 ring-offset-1",
                "text-[11px] font-bold text-white tracking-wider",
                "transition-all hover:ring-zinc-900/20 focus-visible:outline-none"
              )}
            >
              {initials}
            </button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-52">
            <DropdownMenuLabel className="pb-1">
              <p className="text-sm font-medium truncate">{user?.phone ?? "Manager"}</p>
              <Badge
                variant="outline"
                className={cn("mt-1 text-[10px] font-semibold uppercase tracking-wider", ROLE_BADGE[role])}
              >
                {role}
              </Badge>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem asChild>
              <Link href="/profile" className="cursor-pointer">
                Profile
              </Link>
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem
              onClick={logout}
              className="text-rose-600 focus:text-rose-600 focus:bg-rose-50 cursor-pointer gap-2"
            >
              <LogOut className="h-3.5 w-3.5" />
              Sign out
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  );
}
