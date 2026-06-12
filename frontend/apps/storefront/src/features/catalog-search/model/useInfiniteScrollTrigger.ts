"use client";

import { useEffect, useRef } from "react";

export interface UseInfiniteScrollTriggerOptions {
  hasMore?: boolean;
  isLoading: boolean;
  onLoadMore: () => void;
}

/**
 * Returns a ref for a sentinel element. When the sentinel enters the
 * viewport (with a lookahead margin) and more pages are available,
 * `onLoadMore` is called to fetch the next page.
 */
export function useInfiniteScrollTrigger({
  hasMore,
  isLoading,
  onLoadMore,
}: UseInfiniteScrollTriggerOptions) {
  const sentinelRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const sentinel = sentinelRef.current;
    if (!sentinel || !hasMore) return;

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting && hasMore && !isLoading) {
          onLoadMore();
        }
      },
      { rootMargin: "600px" }
    );

    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [hasMore, isLoading, onLoadMore]);

  return sentinelRef;
}
