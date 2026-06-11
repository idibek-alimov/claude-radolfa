"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@radolfa/shared/auth";
import { useHideOnScroll } from "@radolfa/shared/lib/useHideOnScroll";

/** 4-item mobile bottom navigation bar. Hidden on md+. Slides away on scroll-down. */
export default function BottomNav() {
  const pathname = usePathname();
  const { isAuthenticated } = useAuth();
  const hidden = useHideOnScroll();

  const isActive = (href: string) =>
    href === "/" ? pathname === "/" : pathname.startsWith(href);

  const activeClass = "text-mag";
  const inactiveClass = "text-ink/55";

  return (
    <nav
      className={`fixed bottom-0 left-0 right-0 z-30 h-14 bg-white border-t border-ink/8 grid grid-cols-4 text-[10px] font-medium md:hidden transition-transform duration-300 ${hidden ? "translate-y-full" : "translate-y-0"}`}
    >
      {/* Home */}
      <Link
        href="/"
        className={`py-2 flex flex-col items-center gap-0.5 ${isActive("/") ? activeClass : inactiveClass}`}
      >
        <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
          <path d="M3 9.5L12 3l9 6.5V21H3z" />
        </svg>
        Home
      </Link>

      {/* Browse */}
      <Link
        href="/search"
        className={`py-2 flex flex-col items-center gap-0.5 ${isActive("/search") ? activeClass : inactiveClass}`}
      >
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden>
          <circle cx="11" cy="11" r="7" />
          <path d="m21 21-4.3-4.3" />
        </svg>
        Browse
      </Link>

      {/* Bag */}
      <Link
        href="/cart"
        className={`py-2 flex flex-col items-center gap-0.5 ${isActive("/cart") ? activeClass : inactiveClass}`}
      >
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden>
          <path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4Z" />
          <path d="M3 6h18" />
          <path d="M16 10a4 4 0 0 1-8 0" />
        </svg>
        Bag
      </Link>

      {/* Me */}
      <Link
        href={isAuthenticated ? "/profile" : "/login"}
        className={`py-2 flex flex-col items-center gap-0.5 ${isActive("/profile") || isActive("/login") ? activeClass : inactiveClass}`}
      >
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden>
          <circle cx="12" cy="7" r="4" />
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
        </svg>
        Me
      </Link>
    </nav>
  );
}
