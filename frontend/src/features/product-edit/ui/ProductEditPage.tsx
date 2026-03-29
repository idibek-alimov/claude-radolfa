"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { ChevronRight, Loader2, AlertCircle } from "lucide-react";
import { AnimatePresence, motion } from "framer-motion";
import { useAuth } from "@/features/auth";
import { fetchListingBySlug } from "@/entities/product/api";
import { GeneralInfoCard } from "./GeneralInfoCard";
import { SkuTableCard } from "./SkuTableCard";
import { EnrichmentCard } from "./EnrichmentCard";
import { ImageCard } from "./ImageCard";
import { DimensionsCard } from "./DimensionsCard";
import { EditSectionNav, type SectionKey } from "./EditSectionNav";
import { TagAssignmentCard } from "@/features/tag-management";

interface Props {
  slug: string;
}

const SECTION_ORDER: SectionKey[] = ["general", "skus", "content", "dimensions"];

const slideVariants = {
  enter: (direction: number) => ({
    x: direction > 0 ? 40 : -40,
    opacity: 0,
  }),
  center: { x: 0, opacity: 1 },
  exit: (direction: number) => ({
    x: direction > 0 ? -40 : 40,
    opacity: 0,
  }),
};

export function ProductEditPage({ slug }: Props) {
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  const [activeSection, setActiveSection] = useState<SectionKey>("general");
  const [direction, setDirection] = useState(1);

  const { data: detail, isLoading, isError } = useQuery({
    queryKey: ["listing", slug],
    queryFn: () => fetchListingBySlug(slug),
  });

  function handleSectionChange(key: SectionKey) {
    const from = SECTION_ORDER.indexOf(activeSection);
    const to = SECTION_ORDER.indexOf(key);
    setDirection(to > from ? 1 : -1);
    setActiveSection(key);
  }

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-24 text-muted-foreground gap-2">
        <Loader2 className="h-5 w-5 animate-spin" />
        <span>Loading…</span>
      </div>
    );
  }

  if (isError || !detail) {
    return (
      <div className="flex items-center justify-center py-24 text-destructive gap-2">
        <AlertCircle className="h-5 w-5" />
        <span>Failed to load product.</span>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50/80 flex flex-col">
      {/* Breadcrumb header */}
      <header className="sticky top-0 z-40 h-14 bg-white border-b flex items-center px-6 gap-2 shrink-0">
        <Link
          href="/manage"
          className="text-sm text-muted-foreground hover:text-foreground transition-colors"
        >
          Products
        </Link>
        <ChevronRight className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
        <span className="text-sm text-muted-foreground truncate max-w-[200px]">
          {detail.colorDisplayName}
        </span>
        <ChevronRight className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
        <span className="text-sm font-medium text-foreground">Edit</span>
      </header>

      {/* Body: sidebar + content */}
      <div className="flex flex-1">
        {/* Left sidebar */}
        <aside className="sticky top-14 h-[calc(100vh-3.5rem)] w-[220px] shrink-0 bg-white border-r overflow-y-auto">
          <EditSectionNav activeSection={activeSection} onSelect={handleSectionChange} />
        </aside>

        {/* Main content */}
        <main className="flex-1 min-w-0 px-8 py-6">
          <AnimatePresence mode="wait" custom={direction}>
            <motion.div
              key={activeSection}
              custom={direction}
              variants={slideVariants}
              initial="enter"
              animate="center"
              exit="exit"
              transition={{ duration: 0.18, ease: "easeInOut" }}
              className="space-y-6"
            >
              {activeSection === "general" && <GeneralInfoCard detail={detail} />}
              {activeSection === "skus" && (
                <SkuTableCard slug={slug} skus={detail.skus} isAdmin={isAdmin} />
              )}
              {activeSection === "content" && (
                <>
                  <EnrichmentCard detail={detail} />
                  <TagAssignmentCard
                    variantId={detail.variantId}
                    variantSlug={detail.slug}
                    currentTags={detail.tags}
                  />
                  <ImageCard slug={slug} images={detail.images} />
                </>
              )}
              {activeSection === "dimensions" && <DimensionsCard detail={detail} />}
            </motion.div>
          </AnimatePresence>
        </main>
      </div>
    </div>
  );
}
