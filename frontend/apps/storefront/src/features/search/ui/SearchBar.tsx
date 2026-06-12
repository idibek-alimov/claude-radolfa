"use client";

import { useState, useEffect, useCallback, useRef } from "react";
import { useRouter } from "next/navigation";
import { Loader2 } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { fetchAutocomplete } from "@/entities/product";
import type { SearchParams } from "@/features/search";
import { useTranslations } from "next-intl";

interface SearchBarProps {
  onSearch?: (params: SearchParams) => void;
  /** compact = desktop pill with inline Search button */
  compact?: boolean;
}

export default function SearchBar({ onSearch, compact = false }: SearchBarProps) {
  const t = useTranslations("search");
  const router = useRouter();
  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [isOpen, setIsOpen] = useState(false);
  const [selectedIdx, setSelectedIdx] = useState(-1);
  const [dismissed, setDismissed] = useState(false);
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
    setIsOpen(!dismissed && suggestions.length > 0 && debouncedQuery.length >= 2);
    setSelectedIdx(-1);
  }, [suggestions, debouncedQuery, dismissed]);

  // Close dropdown on outside click
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target as Node)) {
        setIsOpen(false);
        setDismissed(true);
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
      setDismissed(true);
      inputRef.current?.blur();
    },
    [onSearch, router],
  );

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Escape") {
      setIsOpen(false);
      setDismissed(true);
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
          prev < suggestions.length - 1 ? prev + 1 : 0,
        );
        break;
      case "ArrowUp":
        e.preventDefault();
        setSelectedIdx((prev) =>
          prev > 0 ? prev - 1 : suggestions.length - 1,
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

  // ── Desktop compact: pill with inline Search button ──────────────────────
  if (compact) {
    return (
      <div ref={wrapperRef} className="relative w-full max-w-3xl">
        {/* Search icon */}
        <svg
          width="18"
          height="18"
          viewBox="0 0 24 24"
          fill="none"
          stroke="#9A0E81"
          strokeWidth="2.2"
          className="absolute left-4 top-1/2 -translate-y-1/2 pointer-events-none"
          aria-hidden
        >
          <circle cx="11" cy="11" r="7" />
          <path d="m21 21-4.3-4.3" />
        </svg>

        <input
          ref={inputRef}
          type="text"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setDismissed(false);
          }}
          onKeyDown={handleKeyDown}
          onFocus={() => {
            if (!dismissed && suggestions.length > 0) setIsOpen(true);
          }}
          placeholder={t("placeholder")}
          className="w-full h-12 pl-12 pr-32 rounded-full bg-plum/40 text-[14px] focus:outline-none focus:bg-white focus:ring-2 focus:ring-mag border border-transparent focus:border-mag/40"
          role="combobox"
          aria-expanded={isOpen}
          aria-autocomplete="list"
          aria-controls="search-suggestions-desktop"
        />

        {isFetching && (
          <Loader2 className="absolute right-[7.5rem] top-1/2 -translate-y-1/2 h-4 w-4 text-maglo animate-spin" />
        )}

        <button
          onClick={() => submitSearch(query)}
          className="absolute right-1.5 top-1/2 -translate-y-1/2 h-9 px-5 rounded-full bg-mag text-white text-[13px] font-bold hover:bg-maglo transition-colors"
        >
          {t("submit")}
        </button>

        {/* Autocomplete dropdown */}
        {isOpen && suggestions.length > 0 && (
          <ul
            id="search-suggestions-desktop"
            role="listbox"
            className="absolute z-50 mt-1.5 w-full rounded-2xl border bg-white shadow-xl overflow-hidden"
          >
            {suggestions.map((suggestion, idx) => (
              <li
                key={suggestion}
                role="option"
                aria-selected={idx === selectedIdx}
                onClick={() => handleSuggestionClick(suggestion)}
                onMouseEnter={() => setSelectedIdx(idx)}
                className={`flex items-center gap-3 px-4 py-2.5 text-sm cursor-pointer transition-colors ${
                  idx === selectedIdx ? "bg-plum/40" : "hover:bg-plum/20"
                }`}
              >
                <svg
                  width="13"
                  height="13"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="#9A0E81"
                  strokeWidth="2.2"
                  className="shrink-0"
                  aria-hidden
                >
                  <circle cx="11" cy="11" r="7" />
                  <path d="m21 21-4.3-4.3" />
                </svg>
                <span className="flex-1 truncate">{suggestion}</span>
              </li>
            ))}
          </ul>
        )}
      </div>
    );
  }

  // ── Mobile: full-width pill, no inline button (Enter to submit) ───────────
  return (
    <div ref={wrapperRef} className="relative w-full">
      {/* Search icon */}
      <svg
        width="16"
        height="16"
        viewBox="0 0 24 24"
        fill="none"
        stroke="#9A0E81"
        strokeWidth="2.2"
        className="absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none"
        aria-hidden
      >
        <circle cx="11" cy="11" r="7" />
        <path d="m21 21-4.3-4.3" />
      </svg>

      <input
        ref={inputRef}
        type="text"
        value={query}
        onChange={(e) => {
          setQuery(e.target.value);
          setDismissed(false);
        }}
        onKeyDown={handleKeyDown}
        onFocus={() => {
          if (!dismissed && suggestions.length > 0) setIsOpen(true);
        }}
        placeholder={t("placeholder")}
        className="w-full h-10 pl-9 pr-3 rounded-full bg-plum/60 text-[13px] focus:outline-none focus:bg-white focus:ring-2 focus:ring-mag"
        role="combobox"
        aria-expanded={isOpen}
        aria-autocomplete="list"
        aria-controls="search-suggestions-mobile"
      />

      {isFetching && (
        <Loader2 className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 text-maglo animate-spin" />
      )}

      {/* Autocomplete dropdown */}
      {isOpen && suggestions.length > 0 && (
        <ul
          id="search-suggestions-mobile"
          role="listbox"
          className="absolute z-50 mt-1.5 w-full rounded-xl border bg-white shadow-xl overflow-hidden"
        >
          {suggestions.map((suggestion, idx) => (
            <li
              key={suggestion}
              role="option"
              aria-selected={idx === selectedIdx}
              onClick={() => handleSuggestionClick(suggestion)}
              onMouseEnter={() => setSelectedIdx(idx)}
              className={`flex items-center gap-3 px-4 py-3 text-[15px] cursor-pointer transition-colors ${
                idx === selectedIdx ? "bg-plum/40" : "hover:bg-plum/20"
              }`}
            >
              <svg
                width="13"
                height="13"
                viewBox="0 0 24 24"
                fill="none"
                stroke="#9A0E81"
                strokeWidth="2.2"
                className="shrink-0"
                aria-hidden
              >
                <circle cx="11" cy="11" r="7" />
                <path d="m21 21-4.3-4.3" />
              </svg>
              <span className="flex-1 truncate">{suggestion}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
