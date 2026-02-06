"use client";

import { useState, useEffect, useCallback } from "react";
import { Search } from "lucide-react";
import type { SearchParams } from "@/features/search";

interface SearchBarProps {
  onSearch: (params: SearchParams) => void;
}

export default function SearchBar({ onSearch }: SearchBarProps) {
  const [query, setQuery] = useState("");

  const debouncedSearch = useCallback(
    (() => {
      let timer: NodeJS.Timeout;
      return (value: string) => {
        clearTimeout(timer);
        timer = setTimeout(() => {
          const trimmed = value.trim();
          if (trimmed) {
            onSearch({ query: trimmed });
          }
        }, 300);
      };
    })(),
    [onSearch]
  );

  useEffect(() => {
    if (query) {
      debouncedSearch(query);
    }
  }, [query, debouncedSearch]);

  return (
    <div className="relative w-full max-w-xl">
      <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
      <input
        type="text"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder="Search products..."
        className="w-full pl-10 pr-4 py-2.5 rounded-lg border border-input bg-background focus:outline-none focus:ring-2 focus:ring-ring text-sm"
      />
    </div>
  );
}
