"use client";

import { useState, useRef, useCallback } from "react";
import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { ChevronRight, ChevronDown } from "lucide-react";
import { fetchCategoryTree } from "@/entities/product/api";
import { Skeleton } from "@/shared/ui/skeleton";
import type { CategoryTree } from "@/entities/product/model/types";

/* ── Desktop MegaMenu ──────────────────────────────────────────── */

export function MegaMenu() {
  const { data: categories, isLoading } = useQuery({
    queryKey: ["categories", "tree"],
    queryFn: fetchCategoryTree,
    staleTime: 30 * 60 * 1000,
  });

  const [activeL1, setActiveL1] = useState<string | null>(null);
  const [activeL2, setActiveL2] = useState<string | null>(null);
  const closeTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const cancelClose = useCallback(() => {
    if (closeTimer.current) {
      clearTimeout(closeTimer.current);
      closeTimer.current = null;
    }
  }, []);

  const startClose = useCallback(() => {
    closeTimer.current = setTimeout(() => {
      setActiveL1(null);
      setActiveL2(null);
    }, 150);
  }, []);

  const handleL1Enter = useCallback(
    (slug: string) => {
      cancelClose();
      setActiveL1(slug);
      setActiveL2(null);
    },
    [cancelClose],
  );

  const handleL2Enter = useCallback(
    (slug: string) => {
      cancelClose();
      setActiveL2(slug);
    },
    [cancelClose],
  );

  if (isLoading) {
    return (
      <div className="hidden md:flex items-center gap-6 h-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        {Array.from({ length: 5 }).map((_, i) => (
          <Skeleton key={i} className="h-4 w-20" />
        ))}
      </div>
    );
  }

  if (!categories?.length) return null;

  const activeL1Cat = categories.find((c) => c.slug === activeL1);

  return (
    <div className="hidden md:block relative" onMouseLeave={startClose}>
      {/* Top-level category bar */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <ul className="flex items-center gap-1 h-10">
          {categories.map((cat) => (
            <li key={cat.id}>
              <Link
                href={`/categories/${cat.slug}/products`}
                className={`px-3 py-1.5 text-sm font-medium rounded-md transition-colors ${
                  activeL1 === cat.slug
                    ? "bg-accent text-foreground"
                    : "text-muted-foreground hover:text-foreground hover:bg-accent/50"
                }`}
                onMouseEnter={() => handleL1Enter(cat.slug)}
              >
                {cat.name}
              </Link>
            </li>
          ))}
        </ul>
      </div>

      {/* Flyout panel */}
      {activeL1Cat && activeL1Cat.children.length > 0 && (
        <div
          className="absolute left-0 right-0 z-50 bg-background border-b shadow-lg"
          onMouseEnter={cancelClose}
          onMouseLeave={startClose}
        >
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
            <div className="flex gap-8">
              {/* Level 2 column */}
              <div className="w-56 shrink-0">
                <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">
                  {activeL1Cat.name}
                </p>
                <ul className="space-y-0.5">
                  {activeL1Cat.children.map((l2) => (
                    <li key={l2.id}>
                      <Link
                        href={`/categories/${l2.slug}/products`}
                        className={`flex items-center justify-between px-3 py-2 text-sm rounded-md transition-colors ${
                          activeL2 === l2.slug
                            ? "bg-accent text-foreground font-medium"
                            : "text-muted-foreground hover:text-foreground hover:bg-accent/50"
                        }`}
                        onMouseEnter={() => handleL2Enter(l2.slug)}
                      >
                        {l2.name}
                        {l2.children.length > 0 && (
                          <ChevronRight className="h-3.5 w-3.5" />
                        )}
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>

              {/* Level 3 column */}
              {activeL2 && (
                <Level3Column
                  parent={activeL1Cat.children.find(
                    (c) => c.slug === activeL2,
                  )}
                />
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function Level3Column({ parent }: { parent: CategoryTree | undefined }) {
  if (!parent || parent.children.length === 0) return null;

  return (
    <div className="flex-1 border-l pl-8">
      <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-3">
        {parent.name}
      </p>
      <ul className="grid grid-cols-2 gap-x-6 gap-y-0.5">
        {parent.children.map((l3) => (
          <li key={l3.id}>
            <Link
              href={`/categories/${l3.slug}/products`}
              className="block px-3 py-2 text-sm text-muted-foreground hover:text-foreground hover:bg-accent/50 rounded-md transition-colors"
            >
              {l3.name}
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}

/* ── Mobile MegaMenu (Accordion) ──────────────────────────────── */

export function MegaMenuMobile() {
  const { data: categories, isLoading } = useQuery({
    queryKey: ["categories", "tree"],
    queryFn: fetchCategoryTree,
    staleTime: 30 * 60 * 1000,
  });

  const [activeL1, setActiveL1] = useState<string | null>(null);
  const [activeL2, setActiveL2] = useState<string | null>(null);

  const toggleL1 = (slug: string) => {
    setActiveL1((prev) => (prev === slug ? null : slug));
    setActiveL2(null);
  };

  const toggleL2 = (slug: string) => {
    setActiveL2((prev) => (prev === slug ? null : slug));
  };

  if (isLoading) {
    return (
      <div className="space-y-2 py-2">
        {Array.from({ length: 4 }).map((_, i) => (
          <Skeleton key={i} className="h-8 w-full" />
        ))}
      </div>
    );
  }

  if (!categories?.length) return null;

  return (
    <div className="space-y-0.5">
      <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider px-2 mb-2">
        Categories
      </p>
      {categories.map((l1) => (
        <div key={l1.id}>
          {/* Level 1 */}
          <div className="flex items-center">
            <Link
              href={`/categories/${l1.slug}/products`}
              className="flex-1 text-sm font-medium text-foreground py-2.5 px-2 rounded-lg hover:bg-accent transition-colors"
            >
              {l1.name}
            </Link>
            {l1.children.length > 0 && (
              <button
                onClick={() => toggleL1(l1.slug)}
                className="p-2 rounded-lg hover:bg-accent transition-colors"
              >
                {activeL1 === l1.slug ? (
                  <ChevronDown className="h-4 w-4 text-muted-foreground" />
                ) : (
                  <ChevronRight className="h-4 w-4 text-muted-foreground" />
                )}
              </button>
            )}
          </div>

          {/* Level 2 */}
          {activeL1 === l1.slug && l1.children.length > 0 && (
            <div className="ml-3 border-l pl-2 space-y-0.5">
              {l1.children.map((l2) => (
                <div key={l2.id}>
                  <div className="flex items-center">
                    <Link
                      href={`/categories/${l2.slug}/products`}
                      className="flex-1 text-sm text-muted-foreground py-2 px-2 rounded-lg hover:bg-accent hover:text-foreground transition-colors"
                    >
                      {l2.name}
                    </Link>
                    {l2.children.length > 0 && (
                      <button
                        onClick={() => toggleL2(l2.slug)}
                        className="p-2 rounded-lg hover:bg-accent transition-colors"
                      >
                        {activeL2 === l2.slug ? (
                          <ChevronDown className="h-3.5 w-3.5 text-muted-foreground" />
                        ) : (
                          <ChevronRight className="h-3.5 w-3.5 text-muted-foreground" />
                        )}
                      </button>
                    )}
                  </div>

                  {/* Level 3 */}
                  {activeL2 === l2.slug && l2.children.length > 0 && (
                    <div className="ml-3 border-l pl-2 space-y-0.5">
                      {l2.children.map((l3) => (
                        <Link
                          key={l3.id}
                          href={`/categories/${l3.slug}/products`}
                          className="block text-sm text-muted-foreground py-1.5 px-2 rounded-lg hover:bg-accent hover:text-foreground transition-colors"
                        >
                          {l3.name}
                        </Link>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      ))}
    </div>
  );
}
