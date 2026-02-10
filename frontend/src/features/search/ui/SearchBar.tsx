"use client";

import { useState, useEffect, useCallback, useRef } from "react";
import { useRouter } from "next/navigation";
import { Search, Loader2 } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { fetchAutocomplete } from "@/entities/product";
import type { SearchParams } from "@/features/search";

interface SearchBarProps {
  onSearch: (params: SearchParams) => void;
}

export default function SearchBar({ onSearch }: SearchBarProps) {
  const router = useRouter();
  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [isOpen, setIsOpen] = useState(false);
  const [selectedIdx, setSelectedIdx] = useState(-1);
  const wrapperRef = useRef<HTMLDivElement>(null);

  // Debounce the query for autocomplete
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedQuery(query.trim());
    }, 250);
    return () => clearTimeout(timer);
  }, [query]);

  // Fetch autocomplete suggestions
  const { data: suggestions = [], isFetching } = useQuery({
    queryKey: ["autocomplete", debouncedQuery],
    queryFn: () => fetchAutocomplete(debouncedQuery, 5),
    enabled: debouncedQuery.length >= 2,
    staleTime: 30_000,
  });

  // Show dropdown when we have suggestions
  useEffect(() => {
    setIsOpen(suggestions.length > 0 && debouncedQuery.length >= 2);
    setSelectedIdx(-1);
  }, [suggestions, debouncedQuery]);

  // Close dropdown on outside click
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const submitSearch = useCallback(
    (value: string) => {
      const trimmed = value.trim();
      if (trimmed) {
        onSearch({ query: trimmed });
        setIsOpen(false);
      }
    },
    [onSearch]
  );

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (!isOpen) {
      if (e.key === "Enter") submitSearch(query);
      return;
    }

    switch (e.key) {
      case "ArrowDown":
        e.preventDefault();
        setSelectedIdx((prev) =>
          prev < suggestions.length - 1 ? prev + 1 : 0
        );
        break;
      case "ArrowUp":
        e.preventDefault();
        setSelectedIdx((prev) =>
          prev > 0 ? prev - 1 : suggestions.length - 1
        );
        break;
      case "Enter":
        e.preventDefault();
        if (selectedIdx >= 0 && suggestions[selectedIdx]) {
          setQuery(suggestions[selectedIdx]);
          submitSearch(suggestions[selectedIdx]);
        } else {
          submitSearch(query);
        }
        break;
      case "Escape":
        setIsOpen(false);
        break;
    }
  };

  const handleSuggestionClick = (suggestion: string) => {
    setQuery(suggestion);
    submitSearch(suggestion);
  };

  return (
    <div ref={wrapperRef} className="relative w-full max-w-xl">
      <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
      {isFetching && (
        <Loader2 className="absolute right-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground animate-spin" />
      )}
      <input
        type="text"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        onKeyDown={handleKeyDown}
        onFocus={() => suggestions.length > 0 && setIsOpen(true)}
        placeholder="Search products..."
        className="w-full pl-10 pr-10 py-2.5 rounded-lg border border-input bg-background focus:outline-none focus:ring-2 focus:ring-ring text-sm"
        role="combobox"
        aria-expanded={isOpen}
        aria-autocomplete="list"
        aria-controls="search-suggestions"
      />

      {/* Autocomplete dropdown */}
      {isOpen && suggestions.length > 0 && (
        <ul
          id="search-suggestions"
          role="listbox"
          className="absolute z-50 mt-1 w-full rounded-lg border bg-popover shadow-lg overflow-hidden"
        >
          {suggestions.map((suggestion, idx) => (
            <li
              key={suggestion}
              role="option"
              aria-selected={idx === selectedIdx}
              onClick={() => handleSuggestionClick(suggestion)}
              onMouseEnter={() => setSelectedIdx(idx)}
              className={`flex items-center gap-3 px-4 py-2.5 text-sm cursor-pointer transition-colors ${idx === selectedIdx
                  ? "bg-accent text-accent-foreground"
                  : "text-popover-foreground hover:bg-accent/50"
                }`}
            >
              <Search className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
              <span>{suggestion}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
