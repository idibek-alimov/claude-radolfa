import Link from "next/link";
import type { NavItem } from "@/widgets/Navbar";

const NAV_ITEMS: NavItem[] = [
  { label: "Home", href: "/" },
  { label: "Products", href: "/products" },
  { label: "Search", href: "/products?search=true" },
  { label: "Login", href: "/login" },
];

/**
 * Sticky top navigation bar.
 * Server component — no client-side state needed for the static link set.
 */
export default function Navbar() {
  return (
    <nav className="sticky top-0 z-40 bg-white border-b border-gray-200 shadow-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Brand */}
          <Link href="/" className="text-xl font-bold text-indigo-600">
            Radolfa
          </Link>

          {/* Navigation links */}
          <ul className="flex items-center gap-6">
            {NAV_ITEMS.map((item) => (
              <li key={item.href}>
                <Link
                  href={item.href}
                  className="text-sm font-medium text-gray-700 hover:text-indigo-600 transition-colors"
                >
                  {item.label}
                </Link>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </nav>
  );
}
