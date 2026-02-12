"use client";

import { useState, useEffect, useCallback, useRef } from "react";
import { useRouter } from "next/navigation";
import { Search, Loader2 } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { fetchAutocomplete } from "@/entities/product";
import type { SearchParams } from "@/features/search";

interface SearchBarProps {
  onSearch?: (params: SearchParams) => void;
  compact?: boolean;
}

export default function SearchBar({ onSearch, compact = false }: SearchBarProps) {
  const router = useRouter();
  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [isOpen, setIsOpen] = useState(false);
  const [selectedIdx, setSelectedIdx] = useState(-1);
  const wrapperRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

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
    queryFn: () => fetchAutocomplete(debouncedQuery, 6),
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
      if (!trimmed) return;

      if (onSearch) {
        onSearch({ query: trimmed });
      } else {
        router.push(`/search?q=${encodeURIComponent(trimmed)}`);
      }
      setIsOpen(false);
      inputRef.current?.blur();
    },
    [onSearch, router]
  );

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Escape") {
      setIsOpen(false);
      inputRef.current?.blur();
      return;
    }

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
    }
  };

  const handleSuggestionClick = (suggestion: string) => {
    setQuery(suggestion);
    submitSearch(suggestion);
  };

  // Desktop compact mode: always-expanded input
  if (compact) {
    return (
      <div ref={wrapperRef} className="relative w-full max-w-2xl">
        <div className="flex items-center rounded-xl border border-border bg-background ring-1 ring-ring/20">
          <div className="flex items-center justify-center pl-3.5 pr-1 text-muted-foreground">
            <Search className="h-4 w-4" />
          </div>

          <input
            ref={inputRef}
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={handleKeyDown}
            onFocus={() => {
              if (suggestions.length > 0) setIsOpen(true);
            }}
            placeholder="Search products..."
            className="flex-1 bg-transparent outline-none text-foreground placeholder:text-muted-foreground/60 py-2 text-sm"
            role="combobox"
            aria-expanded={isOpen}
            aria-autocomplete="list"
            aria-controls="search-suggestions"
          />

          {isFetching && (
            <Loader2 className="h-4 w-4 text-muted-foreground animate-spin mr-3 shrink-0" />
          )}
        </div>

        {/* Autocomplete dropdown */}
        {isOpen && suggestions.length > 0 && (
          <ul
            id="search-suggestions"
            role="listbox"
            className="absolute z-50 mt-1.5 w-full rounded-xl border bg-popover shadow-xl overflow-hidden"
          >
            {suggestions.map((suggestion, idx) => (
              <li
                key={suggestion}
                role="option"
                aria-selected={idx === selectedIdx}
                onClick={() => handleSuggestionClick(suggestion)}
                onMouseEnter={() => setSelectedIdx(idx)}
                className={`flex items-center gap-3 px-4 py-2.5 text-sm cursor-pointer transition-colors ${
                  idx === selectedIdx
                    ? "bg-accent"
                    : "hover:bg-muted/60"
                }`}
              >
                <Search className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
                <span className="flex-1 truncate">{suggestion}</span>
              </li>
            ))}
          </ul>
        )}
      </div>
    );
  }

  // Mobile: always-expanded full-width input
  return (
    <div ref={wrapperRef} className="relative w-full">
      <div className="flex items-center rounded-xl border border-border bg-background ring-1 ring-ring/20">
        <div className="flex items-center justify-center pl-3.5 pr-1 text-muted-foreground">
          <Search className="h-[18px] w-[18px]" />
        </div>

        <input
          ref={inputRef}
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={handleKeyDown}
          onFocus={() => {
            if (suggestions.length > 0) setIsOpen(true);
          }}
          placeholder="Search products..."
          className="flex-1 bg-transparent outline-none text-foreground placeholder:text-muted-foreground/60 py-2.5 text-[15px]"
          role="combobox"
          aria-expanded={isOpen}
          aria-autocomplete="list"
          aria-controls="search-suggestions"
        />

        {isFetching && (
          <Loader2 className="h-4 w-4 text-muted-foreground animate-spin mr-3 shrink-0" />
        )}
      </div>

      {/* Autocomplete dropdown */}
      {isOpen && suggestions.length > 0 && (
        <ul
          id="search-suggestions"
          role="listbox"
          className="absolute z-50 mt-1.5 w-full rounded-xl border bg-popover shadow-xl overflow-hidden"
        >
          {suggestions.map((suggestion, idx) => (
            <li
              key={suggestion}
              role="option"
              aria-selected={idx === selectedIdx}
              onClick={() => handleSuggestionClick(suggestion)}
              onMouseEnter={() => setSelectedIdx(idx)}
              className={`flex items-center gap-3 px-4 py-3 text-[15px] cursor-pointer transition-colors ${
                idx === selectedIdx
                  ? "bg-accent"
                  : "hover:bg-muted/60"
              }`}
            >
              <Search className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
              <span className="flex-1 truncate">{suggestion}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
