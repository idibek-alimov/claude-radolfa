"use client";

import { useEffect, useRef, useState } from "react";
import { TagAssignmentCard } from "@/entities/tag";
import { SkuTableCard } from "./SkuTableCard";
import { EnrichmentCard } from "./EnrichmentCard";
import { DimensionsCard } from "./DimensionsCard";
import { ImageCard } from "./ImageCard";
import type { ProductCardVariant } from "@/entities/product/model/types";

interface Props {
  productBaseId: number;
  variant: ProductCardVariant;
  isAdmin: boolean;
}

const SECTIONS = [
  { id: "images",      label: "Images" },
  { id: "skus",        label: "SKUs" },
  { id: "description", label: "Description" },
  { id: "attributes",  label: "Attributes" },
  { id: "dimensions",  label: "Dimensions" },
  { id: "tags",        label: "Tags" },
] as const;

type SectionId = typeof SECTIONS[number]["id"];

export function VariantPanel({ productBaseId, variant, isAdmin }: Props) {
  const [activeSection, setActiveSection] = useState<SectionId>("images");
  const sectionRefs = useRef<Partial<Record<SectionId, HTMLElement>>>({});
  const observerRef = useRef<IntersectionObserver | null>(null);

  useEffect(() => {
    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            setActiveSection(entry.target.id as SectionId);
          }
        }
      },
      { rootMargin: "-20% 0px -60% 0px", threshold: 0 }
    );

    SECTIONS.forEach(({ id }) => {
      const el = sectionRefs.current[id];
      if (el) observer.observe(el);
    });

    observerRef.current = observer;
    return () => observer.disconnect();
  }, []);

  function scrollTo(sectionId: SectionId) {
    sectionRefs.current[sectionId]?.scrollIntoView({ behavior: "smooth", block: "start" });
  }

  return (
    <div className="flex gap-0 flex-1 min-h-0">
      {/* Left anchor nav */}
      <aside className="w-[160px] shrink-0 sticky top-14 bg-white border-r py-5 px-3">
        <nav className="space-y-0.5">
          {SECTIONS.map(({ id, label }) => (
            <button
              key={id}
              type="button"
              onClick={() => scrollTo(id)}
              className={`
                w-full text-left text-sm px-3 py-2 rounded-lg transition-colors
                ${activeSection === id
                  ? "bg-primary/10 text-primary font-medium"
                  : "text-muted-foreground hover:text-foreground hover:bg-muted/50"
                }
              `}
            >
              {label}
            </button>
          ))}
        </nav>
      </aside>

      {/* Scrollable content — no overflow-y-auto; admin shell is the scroll root */}
      <main className="flex-1 min-w-0 px-8 py-6 space-y-8">
        <section
          id="images"
          ref={(el) => { if (el) sectionRefs.current.images = el; }}
        >
          <ImageCard
            slug={variant.slug}
            variantId={variant.variantId}
            productBaseId={productBaseId}
            images={variant.images}
          />
        </section>

        <section
          id="skus"
          ref={(el) => { if (el) sectionRefs.current.skus = el; }}
        >
          <SkuTableCard
            slug={variant.slug}
            productBaseId={productBaseId}
            variantId={variant.variantId}
            skus={variant.skus}
            isAdmin={isAdmin}
          />
        </section>

        <section
          id="description"
          ref={(el) => { if (el) sectionRefs.current.description = el; }}
        >
          <EnrichmentCard variantId={variant.variantId} />
        </section>

        <section
          id="attributes"
          ref={(el) => { if (el) sectionRefs.current.attributes = el; }}
        >
          <AttributesCard attributes={variant.attributes} />
        </section>

        <section
          id="dimensions"
          ref={(el) => { if (el) sectionRefs.current.dimensions = el; }}
        >
          <DimensionsCard variantId={variant.variantId} />
        </section>

        <section
          id="tags"
          ref={(el) => { if (el) sectionRefs.current.tags = el; }}
        >
          <TagAssignmentCard
            variantId={variant.variantId}
            variantSlug={variant.slug}
            productBaseId={productBaseId}
            currentTags={variant.tags}
          />
        </section>
      </main>
    </div>
  );
}

// ── Inline Attributes read-only card ──────────────────────────────────────────

interface AttributesCardProps {
  attributes: { key: string; values: string[] }[];
}

function AttributesCard({ attributes }: AttributesCardProps) {
  return (
    <div className="bg-card rounded-xl border shadow-sm p-6 space-y-4">
      <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
        Attributes
      </h2>

      {attributes.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          No attributes defined for this variant.
        </p>
      ) : (
        <dl className="grid grid-cols-2 gap-x-6 gap-y-3">
          {attributes.map((attr) => (
            <div key={attr.key}>
              <dt className="text-xs text-muted-foreground">{attr.key}</dt>
              <dd className="text-sm font-medium mt-0.5">
                {attr.values.join(", ") || "—"}
              </dd>
            </div>
          ))}
        </dl>
      )}
    </div>
  );
}
