"use client";

import { useState, useCallback, useEffect } from "react";

const STORAGE_KEY = "radolfa_wishlist";

function getWishlistSet(): Set<string> {
  if (typeof window === "undefined") return new Set();
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? new Set(JSON.parse(raw) as string[]) : new Set();
  } catch {
    return new Set();
  }
}

function persistWishlist(set: Set<string>): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(Array.from(set)));
}

export function useWishlist(slug: string) {
  const [wishlisted, setWishlisted] = useState(false);

  useEffect(() => {
    setWishlisted(getWishlistSet().has(slug));
  }, [slug]);

  const toggle = useCallback(
    (e: React.MouseEvent) => {
      e.preventDefault();
      e.stopPropagation();
      const set = getWishlistSet();
      if (set.has(slug)) {
        set.delete(slug);
      } else {
        set.add(slug);
      }
      persistWishlist(set);
      setWishlisted(set.has(slug));
    },
    [slug]
  );

  return { wishlisted, toggle };
}
