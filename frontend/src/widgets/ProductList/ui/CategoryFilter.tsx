"use client";

import { useRef } from "react";
import { useQuery } from "@tanstack/react-query";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { fetchCategoryTree } from "@/entities/product";
import { useTranslations } from "next-intl";

interface CategoryFilterProps {
  selected: string | null;
  onSelect: (slug: string | null) => void;
}

export default function CategoryFilter({ selected, onSelect }: CategoryFilterProps) {
  const t = useTranslations("common");
  const scrollRef = useRef<HTMLDivElement>(null);

  const { data: categories } = useQuery({
    queryKey: ["categoryTree"],
    queryFn: fetchCategoryTree,
    staleTime: 30 * 60 * 1000,
  });

  const scroll = (direction: "left" | "right") => {
    scrollRef.current?.scrollBy({
      left: direction === "left" ? -200 : 200,
      behavior: "smooth",
    });
  };

  if (!categories?.length) return null;

  return (
    <div className="relative group/scroll">
      {/* Left fade + arrow */}
      <button
        onClick={() => scroll("left")}
        className="absolute left-0 top-0 bottom-0 z-10 w-8 flex items-center justify-center bg-gradient-to-r from-background to-transparent opacity-0 group-hover/scroll:opacity-100 transition-opacity"
        aria-label="Scroll left"
      >
        <ChevronLeft className="h-4 w-4 text-muted-foreground" />
      </button>

      {/* Scrollable pills */}
      <div
        ref={scrollRef}
        className="flex gap-2 overflow-x-auto scrollbar-hide py-1 px-1"
      >
        {/* "All" pill */}
        <button
          onClick={() => onSelect(null)}
          className={`shrink-0 px-4 py-1.5 rounded-full text-sm font-medium transition-colors ${
            selected === null
              ? "bg-primary text-primary-foreground"
              : "bg-muted text-muted-foreground hover:bg-muted/80"
          }`}
        >
          {t("allProducts")}
        </button>

        {categories.map((cat) => (
          <button
            key={cat.slug}
            onClick={() => onSelect(cat.slug)}
            className={`shrink-0 px-4 py-1.5 rounded-full text-sm font-medium transition-colors ${
              selected === cat.slug
                ? "bg-primary text-primary-foreground"
                : "bg-muted text-muted-foreground hover:bg-muted/80"
            }`}
          >
            {cat.name}
          </button>
        ))}
      </div>

      {/* Right fade + arrow */}
      <button
        onClick={() => scroll("right")}
        className="absolute right-0 top-0 bottom-0 z-10 w-8 flex items-center justify-center bg-gradient-to-l from-background to-transparent opacity-0 group-hover/scroll:opacity-100 transition-opacity"
        aria-label="Scroll right"
      >
        <ChevronRight className="h-4 w-4 text-muted-foreground" />
      </button>
    </div>
  );
}
