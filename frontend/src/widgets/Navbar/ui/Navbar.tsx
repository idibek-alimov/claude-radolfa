"use client";

import Link from "next/link";
import { useAuth } from "@/features/auth";
import { useState } from "react";

/**
 * Sticky top navigation bar with authentication-aware UI.
 * 
 * Shows different navigation based on auth state:
 * - Not authenticated: Shows "Login" link
 * - Authenticated (USER): Shows user phone + profile dropdown
 * - Authenticated (MANAGER): Shows user phone + profile dropdown + "Manage Products"
 */
export default function Navbar() {
  const { user, isAuthenticated, isLoading, logout } = useAuth();
  const [showDropdown, setShowDropdown] = useState(false);

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
            <li>
              <Link
                href="/"
                className="text-sm font-medium text-gray-700 hover:text-indigo-600 transition-colors"
              >
                Home
              </Link>
            </li>
            <li>
              <Link
                href="/products"
                className="text-sm font-medium text-gray-700 hover:text-indigo-600 transition-colors"
              >
                Products
              </Link>
            </li>
            <li>
              <Link
                href="/products?search=true"
                className="text-sm font-medium text-gray-700 hover:text-indigo-600 transition-colors"
              >
                Search
              </Link>
            </li>

            {/* Auth-dependent navigation */}
            {isLoading ? (
              <li className="text-sm text-gray-400">Loading...</li>
            ) : isAuthenticated && user ? (
              <>
                {/* Manager-only link */}
                {user.role === "MANAGER" && (
                  <li>
                    <Link
                      href="/manage"
                      className="text-sm font-medium text-purple-600 hover:text-purple-700 transition-colors"
                    >
                      Manage Products
                    </Link>
                  </li>
                )}

                {/* User dropdown */}
                <li className="relative">
                  <button
                    onClick={() => setShowDropdown(!showDropdown)}
                    className="flex items-center gap-2 text-sm font-medium text-gray-700 hover:text-indigo-600 transition-colors"
                  >
                    <span>{user.phone}</span>
                    <span
                      className={`px-2 py-0.5 text-xs rounded ${user.role === "MANAGER"
                          ? "bg-purple-100 text-purple-700"
                          : "bg-blue-100 text-blue-700"
                        }`}
                    >
                      {user.role}
                    </span>
                    <svg
                      className="w-4 h-4"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M19 9l-7 7-7-7"
                      />
                    </svg>
                  </button>

                  {/* Dropdown menu */}
                  {showDropdown && (
                    <div className="absolute right-0 mt-2 w-48 bg-white rounded-md shadow-lg py-1 border border-gray-200">
                      <Link
                        href="/profile"
                        className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                        onClick={() => setShowDropdown(false)}
                      >
                        My Profile
                      </Link>
                      <button
                        onClick={() => {
                          setShowDropdown(false);
                          logout();
                        }}
                        className="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-gray-100"
                      >
                        Logout
                      </button>
                    </div>
                  )}
                </li>
              </>
            ) : (
              <li>
                <Link
                  href="/login"
                  className="text-sm font-medium text-gray-700 hover:text-indigo-600 transition-colors"
                >
                  Login
                </Link>
              </li>
            )}
          </ul>
        </div>
      </div>
    </nav>
  );
}
