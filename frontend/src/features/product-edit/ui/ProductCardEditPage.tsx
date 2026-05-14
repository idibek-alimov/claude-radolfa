"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { ChevronRight, AlertCircle } from "lucide-react";
import { Skeleton } from "@/shared/ui/skeleton";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useAuth } from "@/features/auth";
import { VariantTabBar, ColorPickerDialog } from "@/entities/product";
import { fetchColors } from "@/entities/color";
import { addVariantToProduct } from "@/entities/product/api/admin";
import { getErrorMessage } from "@/shared/lib";
import { useProductCard } from "../model/useProductCard";
import { useProductCardDraft } from "../model/useProductCardDraft";
import { ProductCardDraftProvider } from "../model/ProductCardDraftContext";
import { useCommitDraft } from "../model/useCommitDraft";
import { SharedHeaderCard } from "./SharedHeaderCard";
import { VariantPanel } from "./VariantPanel";
import { SaveBar } from "./SaveBar";
import { DiscardChangesDialog } from "./DiscardChangesDialog";
import { ProductCampaignsPanel } from "@/widgets/product-campaigns-panel";

interface Props {
  productBaseId: number;
}

export function ProductCardEditPage({ productBaseId }: Props) {
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  const router = useRouter();
  const searchParams = useSearchParams();
  const variantParam = searchParams.get("variant");
  const qc = useQueryClient();

  const { data: card, isLoading, isError } = useProductCard(productBaseId);

  const { data: colors = [] } = useQuery({
    queryKey: ["colors"],
    queryFn: fetchColors,
  });

  const [colorPickerOpen, setColorPickerOpen] = useState(false);
  const [switchGuardOpen, setSwitchGuardOpen] = useState(false);
  const [pendingSwitchSlug, setPendingSwitchSlug] = useState<string | null>(null);

  const activeVariant =
    card?.variants.find((v) => v.slug === variantParam) ?? card?.variants[0];

  function doSwitch(slug: string) {
    const params = new URLSearchParams(searchParams.toString());
    params.set("variant", slug);
    router.replace(`?${params.toString()}`, { scroll: false });
  }

  // ── Draft + commit ─────────────────────────────────────────────────────────
  // These must be called unconditionally (hooks rules), so we initialise with a
  // placeholder that gets replaced once card loads.
  const draftApi = useProductCardDraft(
    card ?? {
      productBaseId,
      name: "",
      brand: null,
      categoryId: null,
      categoryName: null,
      variants: [],
    }
  );
  const { commit, isSaving } = useCommitDraft(productBaseId);

  // Guard beforeunload when dirty
  useEffect(() => {
    if (!draftApi.isDirty) return;
    function handleBeforeUnload(e: BeforeUnloadEvent) {
      e.preventDefault();
    }
    window.addEventListener("beforeunload", handleBeforeUnload);
    return () => window.removeEventListener("beforeunload", handleBeforeUnload);
  }, [draftApi.isDirty]);

  const handleVariantSelect = useCallback(
    (slug: string) => {
      if (draftApi.isDirty) {
        setPendingSwitchSlug(slug);
        setSwitchGuardOpen(true);
      } else {
        doSwitch(slug);
      }
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [draftApi.isDirty, searchParams]
  );

  function handleDiscardAndSwitch() {
    draftApi.reset();
    setSwitchGuardOpen(false);
    if (pendingSwitchSlug) {
      doSwitch(pendingSwitchSlug);
      setPendingSwitchSlug(null);
    }
  }

  async function handleSave() {
    if (!card) return;
    const variantSlugs = new Set(card.variants.map((v) => v.slug));
    await commit(draftApi.diff, variantSlugs);
  }

  // ── Add Color ──────────────────────────────────────────────────────────────
  const addVariantMutation = useMutation({
    mutationFn: (colorId: number) => addVariantToProduct(productBaseId, colorId),
    onSuccess: ({ slug }) => {
      qc.invalidateQueries({ queryKey: ["admin-product", productBaseId] });
      setColorPickerOpen(false);
      doSwitch(slug);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const usedColorIds = new Set(card?.variants.map((v) => v.colorId) ?? []);

  // ── Loading / error states ─────────────────────────────────────────────────

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50/80 flex flex-col">
        <header className="sticky top-0 z-40 h-14 bg-white border-b flex items-center px-6 gap-2 shrink-0">
          <Skeleton className="h-4 w-16" />
          <ChevronRight className="h-3.5 w-3.5 text-muted-foreground/30 shrink-0" />
          <Skeleton className="h-4 w-32" />
          <ChevronRight className="h-3.5 w-3.5 text-muted-foreground/30 shrink-0" />
          <Skeleton className="h-4 w-10" />
        </header>
        <div className="px-8 py-5 bg-card border-b">
          <Skeleton className="h-5 w-24 mb-4" />
          <div className="grid grid-cols-2 gap-5">
            <Skeleton className="h-9" />
            <Skeleton className="h-9" />
          </div>
        </div>
        <div className="px-8 py-4 bg-card border-b flex gap-2">
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-8 w-28 rounded-lg" />
          ))}
        </div>
        <div className="flex flex-1 gap-0">
          <Skeleton className="w-[160px] shrink-0 h-screen" />
          <div className="flex-1 px-8 py-6 space-y-6">
            <Skeleton className="h-64 rounded-xl" />
            <Skeleton className="h-48 rounded-xl" />
          </div>
        </div>
      </div>
    );
  }

  if (isError || !card) {
    return (
      <div className="flex items-center justify-center py-24 text-destructive gap-2">
        <AlertCircle className="h-5 w-5" />
        <span>Failed to load product.</span>
      </div>
    );
  }

  if (!activeVariant) {
    return (
      <div className="flex items-center justify-center py-24 text-muted-foreground gap-2">
        <AlertCircle className="h-5 w-5" />
        <span>This product has no variants yet.</span>
      </div>
    );
  }

  return (
    <ProductCardDraftProvider value={draftApi}>
      <div className="min-h-screen bg-gray-50/80 flex flex-col">
        {/* Sticky breadcrumb header */}
        <header className="sticky top-0 z-40 h-14 bg-white border-b flex items-center px-6 gap-2 shrink-0">
          <Link
            href="/manage"
            className="text-sm text-muted-foreground hover:text-foreground transition-colors"
          >
            Products
          </Link>
          <ChevronRight className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
          <span className="text-sm text-muted-foreground truncate max-w-[200px]">
            {card.name}
          </span>
          <ChevronRight className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
          <span className="text-sm font-medium text-foreground">Edit</span>
        </header>

        {/* Shared header — mutates ProductBase fields */}
        <SharedHeaderCard card={card} />

        {/* Variant pill-bar */}
        <div className="bg-card border-b px-8 py-3">
          <VariantTabBar<string>
            items={card.variants.map((v) => ({
              id: v.slug,
              colorHex: v.colorHex,
              label: v.colorDisplayName,
              subtitle: `${v.skus.length} SKU${v.skus.length !== 1 ? "s" : ""}`,
            }))}
            activeId={activeVariant.slug}
            onSelect={handleVariantSelect}
            onAdd={() => setColorPickerOpen(true)}
          />
        </div>

        {/* Per-variant scrollable panel */}
        <VariantPanel
          key={activeVariant.slug}
          productBaseId={productBaseId}
          variant={activeVariant}
          isAdmin={isAdmin}
        />

        {/* Active discount campaigns panel */}
        <div className="px-8 pb-6">
          <ProductCampaignsPanel
            productBaseId={productBaseId}
            allSkuCodes={card.variants.flatMap((v) => v.skus.map((s) => s.skuCode))}
          />
        </div>

        {/* Sticky save bar — rendered inside the provider */}
        <SaveBar
          onSave={handleSave}
          onDiscard={draftApi.reset}
          isSaving={isSaving}
        />

        {/* Add Color dialog */}
        <ColorPickerDialog
          open={colorPickerOpen}
          onClose={() => setColorPickerOpen(false)}
          colors={colors}
          usedColorIds={usedColorIds}
          onSelect={(colorId) => addVariantMutation.mutate(colorId)}
        />

        {/* Discard-changes guard dialog */}
        <DiscardChangesDialog
          open={switchGuardOpen}
          onConfirm={handleDiscardAndSwitch}
          onCancel={() => {
            setSwitchGuardOpen(false);
            setPendingSwitchSlug(null);
          }}
        />
      </div>
    </ProductCardDraftProvider>
  );
}
