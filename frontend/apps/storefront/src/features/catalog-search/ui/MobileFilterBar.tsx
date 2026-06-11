"use client";

interface MobileFilterBarProps {
  filterCount: number;
  onOpenFilters: () => void;
  onOpenSort: () => void;
}

export function MobileFilterBar({
  filterCount,
  onOpenFilters,
  onOpenSort,
}: MobileFilterBarProps) {
  return (
    <div className="fixed left-0 right-0 bottom-0 z-30 bg-white border-t border-ink/10 p-3 flex gap-3">
      <button
        onClick={onOpenFilters}
        className="flex-1 h-11 rounded-full bg-mag text-white font-bold text-[13px] inline-flex items-center justify-center gap-2"
      >
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3" />
        </svg>
        Filters
        {filterCount > 0 && (
          <span className="w-5 h-5 rounded-full bg-white/25 text-[10px] inline-flex items-center justify-center">
            {filterCount}
          </span>
        )}
      </button>
      <button
        onClick={onOpenSort}
        className="flex-1 h-11 rounded-full border-2 border-mag text-mag font-bold text-[13px] inline-flex items-center justify-center gap-2"
      >
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M3 6h18M7 12h10M11 18h2" />
        </svg>
        Sort
      </button>
    </div>
  );
}
