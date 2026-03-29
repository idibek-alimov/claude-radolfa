"use client";

import { cn } from "@/shared/lib/utils";

const SECTIONS = [
  { key: "general",    label: "General Info",    description: "Name, category, color" },
  { key: "skus",       label: "Sizes & Stock",   description: "Size labels, prices, inventory" },
  { key: "content",    label: "Content & Media", description: "Description, flags, images" },
  { key: "dimensions", label: "Dimensions",      description: "Weight, width, height, depth" },
] as const;

export type SectionKey = (typeof SECTIONS)[number]["key"];

interface Props {
  activeSection: SectionKey;
  onSelect: (key: SectionKey) => void;
}

export function EditSectionNav({ activeSection, onSelect }: Props) {
  return (
    <nav className="p-5" aria-label="Product edit sections">
      <p className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground px-1 mb-5">
        Sections
      </p>

      <ol className="space-y-1">
        {SECTIONS.map((section, idx) => {
          const isActive = section.key === activeSection;
          return (
            <li key={section.key}>
              <button
                type="button"
                onClick={() => onSelect(section.key)}
                className={cn(
                  "group w-full flex items-center gap-3 px-2 py-2.5 rounded-lg text-left transition-colors",
                  isActive ? "bg-primary/10" : "hover:bg-muted/60"
                )}
              >
                <div
                  className={cn(
                    "flex h-7 w-7 shrink-0 items-center justify-center rounded-full border-2 text-[11px] font-bold transition-all",
                    isActive
                      ? "border-primary bg-background text-primary shadow-[0_0_0_3px_hsl(var(--primary)/0.12)]"
                      : "border-muted-foreground/25 bg-background text-muted-foreground"
                  )}
                >
                  {idx + 1}
                </div>
                <div className="min-w-0 flex-1">
                  <p
                    className={cn(
                      "text-sm font-medium leading-tight truncate",
                      isActive ? "text-primary" : "text-muted-foreground"
                    )}
                  >
                    {section.label}
                  </p>
                  <p className="text-[11px] text-muted-foreground truncate mt-0.5 leading-tight">
                    {section.description}
                  </p>
                </div>
              </button>
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
