"use client";

import { cn } from "@radolfa/shared/lib/utils";

interface CatalogPaginationProps {
  hasMore?: boolean;
  isFetchingNextPage: boolean;
  onShowMore: () => void;
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  showPager?: boolean;
}

/** Builds a compact page-number window: 1 … current-1, current, current+1 … last */
function buildPageWindow(page: number, totalPages: number): (number | "ellipsis")[] {
  if (totalPages <= 1) return [1];

  const pages = new Set<number>([1, totalPages, page - 1, page, page + 1]);
  const sorted = [...pages].filter((p) => p >= 1 && p <= totalPages).sort((a, b) => a - b);

  const result: (number | "ellipsis")[] = [];
  let prev: number | undefined;
  for (const p of sorted) {
    if (prev != null && p - prev > 1) result.push("ellipsis");
    result.push(p);
    prev = p;
  }
  return result;
}

export function CatalogPagination({
  hasMore,
  isFetchingNextPage,
  onShowMore,
  page,
  totalPages,
  onPageChange,
  showPager,
}: CatalogPaginationProps) {
  const pageWindow = buildPageWindow(page, totalPages);

  return (
    <div className="mt-8 flex flex-col items-center gap-3">
      {hasMore && (
        <button
          onClick={onShowMore}
          disabled={isFetchingNextPage}
          className="h-11 px-8 rounded-full border-2 border-mag text-mag font-bold text-[14px] hover:bg-mag hover:text-white transition disabled:opacity-60"
        >
          {isFetchingNextPage ? "Loading…" : "Show more"}
        </button>
      )}

      {showPager && totalPages > 1 && (
        <div className="flex items-center gap-1.5">
          {pageWindow.map((p, i) =>
            p === "ellipsis" ? (
              <span key={`ellipsis-${i}`} className="px-1 text-ink/40">
                …
              </span>
            ) : (
              <button
                key={p}
                onClick={() => onPageChange(p)}
                className={cn(
                  "h-9 w-9 rounded-full text-[13px]",
                  p === page ? "bg-mag text-white font-bold" : "bg-plum/50 hover:bg-plum"
                )}
              >
                {p}
              </button>
            )
          )}
        </div>
      )}
    </div>
  );
}
