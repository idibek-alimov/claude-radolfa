"use client";

import { useRef, useState, useCallback } from "react";
import Image from "next/image";
import { useQuery } from "@tanstack/react-query";
import {
  Plus,
  X,
  Trash2,
  ImagePlus,
  Loader2,
  AlertCircle,
  RefreshCw,
  Package,
  ChevronsDown,
  Palette,
} from "lucide-react";
import { fetchColors } from "@/entities/color";
import { uploadProductImage } from "../../api/imageUpload";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/shared/ui/alert-dialog";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui/dialog";
import { cn } from "@/shared/lib/utils";
import type {
  WizardState,
  VariantDraft,
  SkuRow,
  Step2Errors,
} from "../../model/types";
import type { Color } from "@/entities/product/model/types";

// ── helpers ──────────────────────────────────────────────────────────

function makeSkuRow(sizeLabel = ""): SkuRow {
  return {
    _key: crypto.randomUUID(),
    sizeLabel,
    price: 0,
    stockQuantity: 0,
    barcode: "",
  };
}

function makeVariant(colorId: number): VariantDraft {
  return {
    colorId,
    images: [],
    skus: [makeSkuRow("ONE_SIZE")],
  };
}

// ── main component ───────────────────────────────────────────────────

interface Props {
  state: WizardState;
  update: (patch: Partial<WizardState>) => void;
  submitted: boolean;
  errors: Step2Errors;
}

export function Step2Variants({ state, update, submitted, errors }: Props) {
  const [activeColorId, setActiveColorId] = useState<number | null>(null);
  const [colorPickerOpen, setColorPickerOpen] = useState(false);
  const [variantToRemove, setVariantToRemove] = useState<number | null>(null);

  const { data: colors = [] } = useQuery({
    queryKey: ["colors"],
    queryFn: fetchColors,
  });

  const colorMap = Object.fromEntries(colors.map((c) => [c.id, c]));
  const usedColorIds = new Set(state.variants.map((v) => v.colorId));

  const currentColorId =
    activeColorId !== null && usedColorIds.has(activeColorId)
      ? activeColorId
      : state.variants[0]?.colorId ?? null;

  const activeVariant = state.variants.find((v) => v.colorId === currentColorId);

  // ── mutation helpers ───────────────────────────────────────────────

  function updateVariant(colorId: number, patch: Partial<VariantDraft>) {
    update({
      variants: state.variants.map((v) =>
        v.colorId === colorId ? { ...v, ...patch } : v
      ),
    });
  }

  function addVariant(colorId: number) {
    update({ variants: [...state.variants, makeVariant(colorId)] });
    setActiveColorId(colorId);
    setColorPickerOpen(false);
  }

  function removeVariant(colorId: number) {
    update({ variants: state.variants.filter((v) => v.colorId !== colorId) });
    if (activeColorId === colorId) setActiveColorId(null);
    setVariantToRemove(null);
  }

  function handleRemoveClick(colorId: number) {
    const variant = state.variants.find((v) => v.colorId === colorId);
    if (variant && variant.images.length > 0) {
      setVariantToRemove(colorId);
    } else {
      removeVariant(colorId);
    }
  }

  function updateSku(colorId: number, key: string, patch: Partial<SkuRow>) {
    const variant = state.variants.find((v) => v.colorId === colorId);
    if (!variant) return;
    updateVariant(colorId, {
      skus: variant.skus.map((r) => (r._key === key ? { ...r, ...patch } : r)),
    });
  }

  function addSku(colorId: number) {
    const variant = state.variants.find((v) => v.colorId === colorId);
    if (!variant) return;
    updateVariant(colorId, { skus: [...variant.skus, makeSkuRow()] });
  }

  function deleteSku(colorId: number, key: string) {
    const variant = state.variants.find((v) => v.colorId === colorId);
    if (!variant) return;
    updateVariant(colorId, {
      skus: variant.skus.filter((r) => r._key !== key),
    });
  }

  function applyDown(colorId: number, field: "price" | "stockQuantity") {
    const variant = state.variants.find((v) => v.colorId === colorId);
    if (!variant || variant.skus.length < 2) return;
    const firstValue = variant.skus[0][field];
    updateVariant(colorId, {
      skus: variant.skus.map((r) => ({ ...r, [field]: firstValue })),
    });
  }

  const removingVariant =
    variantToRemove !== null
      ? state.variants.find((v) => v.colorId === variantToRemove)
      : null;
  const removingColor =
    variantToRemove !== null ? colorMap[variantToRemove] : null;

  // ── render ─────────────────────────────────────────────────────────

  return (
    <div className="max-w-4xl space-y-5">
      {/* Step header */}
      <div className="mb-6">
        <h1 className="text-xl font-semibold text-foreground">Variants &amp; Media</h1>
        <p className="text-sm text-muted-foreground mt-1">
          Add a color variant for each available color. Each variant has its own
          photos and size/stock matrix.
        </p>
      </div>

      {/* ── Variant selector bar ───────────────────────────────────── */}
      <div className="flex flex-wrap items-center gap-2">
        {state.variants.map((variant) => {
          const color = colorMap[variant.colorId];
          const isActive = variant.colorId === currentColorId;
          const hasErrors =
            submitted &&
            variant.skus.some(
              (r) =>
                errors.emptySize.has(r._key) || errors.emptyBarcode.has(r._key)
            );

          return (
            <div key={variant.colorId} className="relative group">
              <button
                type="button"
                onClick={() => setActiveColorId(variant.colorId)}
                className={cn(
                  "flex items-center gap-2 pl-3 pr-8 py-2 rounded-lg border text-sm font-medium transition-all",
                  isActive
                    ? "border-primary bg-primary/5 text-primary shadow-sm"
                    : "border-border bg-white hover:border-primary/40 text-foreground"
                )}
              >
                <span
                  className="h-3.5 w-3.5 rounded-full border border-black/10 shrink-0"
                  style={{ backgroundColor: color?.hexCode ?? "#e5e7eb" }}
                />
                <span className="max-w-[120px] truncate">
                  {color?.displayName ?? color?.colorKey ?? `#${variant.colorId}`}
                </span>
                <span
                  className={cn(
                    "text-xs",
                    isActive ? "text-primary/70" : "text-muted-foreground"
                  )}
                >
                  {variant.skus.length} SKU{variant.skus.length !== 1 ? "s" : ""}
                </span>
                {hasErrors && (
                  <span className="h-2 w-2 rounded-full bg-destructive shrink-0" />
                )}
              </button>
              {/* Remove button — appears on hover */}
              <button
                type="button"
                onClick={() => handleRemoveClick(variant.colorId)}
                className="absolute right-1.5 top-1/2 -translate-y-1/2 h-5 w-5 flex items-center justify-center rounded text-muted-foreground hover:text-destructive hover:bg-destructive/10 transition-colors"
                aria-label={`Remove ${color?.displayName ?? "color"} variant`}
              >
                <X className="h-3 w-3" />
              </button>
            </div>
          );
        })}

        <button
          type="button"
          onClick={() => setColorPickerOpen(true)}
          className="flex items-center gap-1.5 px-3 py-2 rounded-lg border border-dashed border-muted-foreground/40 text-sm text-muted-foreground hover:border-primary/50 hover:text-primary hover:bg-primary/5 transition-colors"
        >
          <Plus className="h-3.5 w-3.5" />
          Add Color
        </button>
      </div>

      {/* ── Empty state ───────────────────────────────────────────── */}
      {state.variants.length === 0 && (
        <div className="bg-white rounded-xl border shadow-sm flex flex-col items-center justify-center gap-3 py-16 text-center">
          <div className="h-12 w-12 rounded-full bg-muted flex items-center justify-center">
            <Package className="h-6 w-6 text-muted-foreground" />
          </div>
          <div>
            <p className="text-sm font-medium text-foreground">No color variants yet</p>
            <p className="text-xs text-muted-foreground mt-1">
              Click <span className="font-medium">Add Color</span> to create the first variant
            </p>
          </div>
        </div>
      )}

      {/* ── Active variant content ────────────────────────────────── */}
      {activeVariant && (
        <div className="space-y-5">
          {/* Photos card */}
          <div className="bg-white rounded-xl border shadow-sm overflow-hidden">
            <div className="px-6 py-4 border-b bg-gray-50/60 flex items-center justify-between">
              <div>
                <h2 className="text-sm font-semibold text-foreground">Photos</h2>
                <p className="text-xs text-muted-foreground mt-0.5">
                  First photo will be the main image. PNG, JPG, WebP.
                </p>
              </div>
              {activeVariant.images.length > 0 && (
                <span className="text-xs text-muted-foreground">
                  {activeVariant.images.length} / 20
                </span>
              )}
            </div>
            <div className="p-5">
              <MediaZone
                images={activeVariant.images}
                onUploaded={(url) =>
                  updateVariant(activeVariant.colorId, {
                    images: [...activeVariant.images, url],
                  })
                }
                onDelete={(url) =>
                  updateVariant(activeVariant.colorId, {
                    images: activeVariant.images.filter((u) => u !== url),
                  })
                }
              />
            </div>
          </div>

          {/* SKU matrix card */}
          <div className="bg-white rounded-xl border shadow-sm overflow-hidden">
            <div className="px-6 py-4 border-b bg-gray-50/60">
              <h2 className="text-sm font-semibold text-foreground">Sizes &amp; Stock</h2>
              <p className="text-xs text-muted-foreground mt-0.5">
                Each row is one purchasable SKU. Size label and barcode are required.
              </p>
            </div>
            <div className="p-5">
              <SkuMatrixTable
                skus={activeVariant.skus}
                submitted={submitted}
                errors={errors}
                onUpdateSku={(key, patch) =>
                  updateSku(activeVariant.colorId, key, patch)
                }
                onAddSku={() => addSku(activeVariant.colorId)}
                onDeleteSku={(key) => deleteSku(activeVariant.colorId, key)}
                onApplyDown={(field) => applyDown(activeVariant.colorId, field)}
              />
            </div>
          </div>
        </div>
      )}

      {/* Color picker dialog */}
      <ColorPickerDialog
        open={colorPickerOpen}
        onClose={() => setColorPickerOpen(false)}
        colors={colors}
        usedColorIds={usedColorIds}
        onSelect={addVariant}
      />

      {/* Remove variant confirmation */}
      <AlertDialog
        open={variantToRemove !== null}
        onOpenChange={(open) => !open && setVariantToRemove(null)}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Remove variant?</AlertDialogTitle>
            <AlertDialogDescription>
              Removing{" "}
              <strong>
                {removingColor?.displayName ??
                  removingColor?.colorKey ??
                  "this color"}
              </strong>{" "}
              will discard {removingVariant?.images.length ?? 0} uploaded{" "}
              {(removingVariant?.images.length ?? 0) === 1 ? "image" : "images"}.
              This cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Keep variant</AlertDialogCancel>
            <AlertDialogAction
              onClick={() =>
                variantToRemove !== null && removeVariant(variantToRemove)
              }
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              Remove anyway
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}

// ── ColorPickerDialog ─────────────────────────────────────────────────

interface ColorPickerDialogProps {
  open: boolean;
  onClose: () => void;
  colors: Color[];
  usedColorIds: Set<number>;
  onSelect: (colorId: number) => void;
}

function ColorPickerDialog({
  open,
  onClose,
  colors,
  usedColorIds,
  onSelect,
}: ColorPickerDialogProps) {
  const available = colors.filter((c) => !usedColorIds.has(c.id));
  const [hoveredId, setHoveredId] = useState<number | null>(null);

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent
        className="max-w-[420px] p-0 gap-0 border-0 shadow-2xl rounded-2xl overflow-hidden bg-white"
        overlayClassName="bg-black/20"
      >
        {/* Header */}
        <div className="px-6 py-5 border-b border-gray-100">
          <DialogHeader>
            <DialogTitle className="text-[15px] font-semibold text-gray-900 tracking-tight">
              Choose a color
            </DialogTitle>
            {available.length > 0 && (
              <p className="text-xs text-gray-400 mt-0.5">
                {available.length} color{available.length !== 1 ? "s" : ""} available
              </p>
            )}
          </DialogHeader>
        </div>

        {/* Content */}
        {available.length === 0 ? (
          <div className="flex flex-col items-center justify-center gap-4 py-14 px-6">
            <div className="h-16 w-16 rounded-2xl bg-gradient-to-b from-gray-50 to-gray-100 border border-gray-100 flex items-center justify-center">
              <Palette className="h-7 w-7 text-gray-300" />
            </div>
            <div className="text-center space-y-1">
              <p className="text-sm font-semibold text-gray-700">All colors assigned</p>
              <p className="text-xs text-gray-400 leading-relaxed max-w-[200px]">
                Every available color has been added as a variant.
              </p>
            </div>
          </div>
        ) : (
          <div className="p-4 max-h-[360px] overflow-y-auto">
            <div className="grid grid-cols-3 gap-2">
              {available.map((color) => {
                const isHovered = hoveredId === color.id;
                const hex = color.hexCode ?? "#e5e7eb";
                return (
                  <button
                    key={color.id}
                    type="button"
                    onClick={() => onSelect(color.id)}
                    onMouseEnter={() => setHoveredId(color.id)}
                    onMouseLeave={() => setHoveredId(null)}
                    className={cn(
                      "flex flex-col items-center gap-2.5 rounded-xl border bg-white p-3.5 text-center transition-all duration-150 active:scale-[0.96]",
                      isHovered ? "-translate-y-0.5 shadow-lg" : "shadow-sm"
                    )}
                    style={{ borderColor: isHovered ? hex : "#f3f4f6" }}
                  >
                    <span
                      className={cn(
                        "h-10 w-10 rounded-full shrink-0 transition-transform duration-150",
                        isHovered ? "scale-110" : "scale-100"
                      )}
                      style={{
                        backgroundColor: hex,
                        boxShadow: isHovered
                          ? `0 0 0 2px white, 0 0 0 4px ${hex}, 0 6px 16px ${hex}50`
                          : `0 0 0 2px white, 0 0 0 3px ${hex}40, 0 2px 8px rgba(0,0,0,0.08)`,
                      }}
                    />
                    <span
                      className={cn(
                        "text-[11px] font-medium leading-tight transition-colors line-clamp-2 w-full",
                        isHovered ? "text-gray-900" : "text-gray-500"
                      )}
                    >
                      {color.displayName ?? color.colorKey}
                    </span>
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {/* Footer */}
        <div className="px-5 py-3 border-t border-gray-50 bg-gray-50/60">
          <p className="text-[10px] text-gray-300 text-center tracking-widest uppercase">
            Click a color to add a variant
          </p>
        </div>
      </DialogContent>
    </Dialog>
  );
}

// ── Toggle ────────────────────────────────────────────────────────────

interface ToggleProps {
  checked: boolean;
  onChange: (value: boolean) => void;
}

function Toggle({ checked, onChange }: ToggleProps) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      onClick={() => onChange(!checked)}
      className={cn(
        "relative inline-flex h-5 w-9 shrink-0 items-center rounded-full transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
        checked ? "bg-primary" : "bg-muted-foreground/30"
      )}
    >
      <span
        className={cn(
          "inline-block h-4 w-4 rounded-full bg-white shadow-sm transition-transform",
          checked ? "translate-x-4" : "translate-x-0.5"
        )}
      />
    </button>
  );
}

// ── MediaZone ─────────────────────────────────────────────────────────

type UploadStatus = "uploading" | "error";

interface MediaZoneProps {
  images: string[];
  onUploaded: (url: string) => void;
  onDelete: (url: string) => void;
}

function MediaZone({ images, onUploaded, onDelete }: MediaZoneProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [uploadStatuses, setUploadStatuses] = useState<
    Record<string, UploadStatus>
  >({});
  const [isDragOver, setIsDragOver] = useState(false);

  function setStatus(id: string, status: UploadStatus | null) {
    setUploadStatuses((prev) => {
      const next = { ...prev };
      if (status === null) delete next[id];
      else next[id] = status;
      return next;
    });
  }

  const processFiles = useCallback(
    async (files: File[]) => {
      for (const file of files) {
        const tempId = crypto.randomUUID();
        setStatus(tempId, "uploading");
        try {
          const { url } = await uploadProductImage(file);
          onUploaded(url);
          setStatus(tempId, null);
        } catch {
          setStatus(tempId, "error");
        }
      }
    },
    [onUploaded]
  );

  function handleFileInput(e: React.ChangeEvent<HTMLInputElement>) {
    const files = Array.from(e.target.files ?? []);
    if (files.length) processFiles(files);
    e.target.value = "";
  }

  function handleDrop(e: React.DragEvent) {
    e.preventDefault();
    setIsDragOver(false);
    const files = Array.from(e.dataTransfer.files).filter((f) =>
      f.type.startsWith("image/")
    );
    if (files.length) processFiles(files);
  }

  const uploadingCount = Object.values(uploadStatuses).filter(
    (s) => s === "uploading"
  ).length;
  const errorIds = Object.entries(uploadStatuses)
    .filter(([, s]) => s === "error")
    .map(([id]) => id);

  const hasImages = images.length > 0 || uploadingCount > 0;

  const dragHandlers = {
    onDragOver: (e: React.DragEvent) => {
      e.preventDefault();
      setIsDragOver(true);
    },
    onDragLeave: () => setIsDragOver(false),
    onDrop: handleDrop,
  };

  return (
    <div className="space-y-3">
      <input
        ref={inputRef}
        type="file"
        accept="image/*"
        multiple
        className="hidden"
        onChange={handleFileInput}
      />

      {/* Upload error banner */}
      {errorIds.length > 0 && (
        <div className="flex items-center gap-2 rounded-lg bg-destructive/10 border border-destructive/20 px-4 py-2.5 text-sm text-destructive">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>
            {errorIds.length}{" "}
            {errorIds.length === 1 ? "image" : "images"} failed to upload
          </span>
          <button
            type="button"
            onClick={() =>
              setUploadStatuses((prev) => {
                const next = { ...prev };
                errorIds.forEach((id) => delete next[id]);
                return next;
              })
            }
            className="ml-auto flex items-center gap-1 text-xs underline underline-offset-2 hover:opacity-70"
          >
            <RefreshCw className="h-3 w-3" />
            Dismiss
          </button>
        </div>
      )}

      {/* Empty drop zone */}
      {!hasImages && (
        <div
          onClick={() => inputRef.current?.click()}
          {...dragHandlers}
          className={cn(
            "flex flex-col items-center justify-center gap-3 rounded-xl border-2 border-dashed p-12 cursor-pointer transition-all",
            isDragOver
              ? "border-primary bg-primary/5 scale-[0.99]"
              : "border-muted-foreground/20 hover:border-primary/40 hover:bg-gray-50"
          )}
        >
          <div
            className={cn(
              "h-12 w-12 rounded-full flex items-center justify-center transition-colors",
              isDragOver ? "bg-primary/10" : "bg-muted"
            )}
          >
            <ImagePlus
              className={cn(
                "h-6 w-6 transition-colors",
                isDragOver ? "text-primary" : "text-muted-foreground"
              )}
            />
          </div>
          <div className="text-center">
            <p className="text-sm font-medium text-foreground">
              Drop photos here or click to browse
            </p>
            <p className="text-xs text-muted-foreground mt-1">
              PNG, JPG, WebP — up to 20 photos
            </p>
          </div>
        </div>
      )}

      {/* Focal image layout — main large + smaller grid */}
      {hasImages && (
        <div className="flex gap-2.5" style={{ minHeight: "200px" }}>
          {/* Main image — large */}
          <div className="w-48 h-48 relative rounded-xl overflow-hidden border bg-gray-50 shrink-0 group">
            {images[0] && (
              <>
                <Image
                  src={images[0]}
                  alt="Main product image"
                  fill
                  className="object-cover"
                  unoptimized
                />
                <span className="absolute bottom-0 inset-x-0 bg-amber-500/90 text-white text-[10px] font-bold text-center py-1 pointer-events-none uppercase tracking-wide">
                  Main
                </span>
                <button
                  type="button"
                  onClick={() => onDelete(images[0])}
                  className="absolute top-1.5 right-1.5 h-6 w-6 rounded-full bg-black/60 text-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity hover:bg-black/80"
                >
                  <X className="h-3 w-3" />
                </button>
              </>
            )}
            {!images[0] && uploadingCount > 0 && (
              <div className="h-full flex items-center justify-center">
                <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
              </div>
            )}
          </div>

          {/* Secondary images grid */}
          <div className="flex-1 grid grid-cols-4 gap-2 content-start">
            {images.slice(1).map((url) => (
              <div
                key={url}
                className="aspect-square relative rounded-lg overflow-hidden border bg-gray-50 group"
              >
                <Image
                  src={url}
                  alt="Product image"
                  fill
                  className="object-cover"
                  unoptimized
                />
                <button
                  type="button"
                  onClick={() => onDelete(url)}
                  className="absolute top-1 right-1 h-5 w-5 rounded-full bg-black/60 text-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity hover:bg-black/80"
                >
                  <X className="h-3 w-3" />
                </button>
              </div>
            ))}

            {/* Uploading placeholders */}
            {Array.from({ length: uploadingCount }).map((_, i) => (
              <div
                key={`uploading-${i}`}
                className="aspect-square rounded-lg border bg-muted flex items-center justify-center"
              >
                <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
              </div>
            ))}

            {/* Add more tile */}
            <div
              onClick={() => inputRef.current?.click()}
              {...dragHandlers}
              className={cn(
                "aspect-square rounded-lg border-2 border-dashed flex flex-col items-center justify-center cursor-pointer transition-colors gap-1",
                isDragOver
                  ? "border-primary bg-primary/5"
                  : "border-muted-foreground/20 hover:border-primary/40 hover:bg-gray-50"
              )}
            >
              <ImagePlus className="h-4 w-4 text-muted-foreground" />
              <span className="text-[10px] text-muted-foreground">Add</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// ── SkuMatrixTable ────────────────────────────────────────────────────

interface SkuMatrixTableProps {
  skus: SkuRow[];
  submitted: boolean;
  errors: Step2Errors;
  onUpdateSku: (key: string, patch: Partial<SkuRow>) => void;
  onAddSku: () => void;
  onDeleteSku: (key: string) => void;
  onApplyDown: (field: "price" | "stockQuantity") => void;
}

function SkuMatrixTable({
  skus,
  submitted,
  errors,
  onUpdateSku,
  onAddSku,
  onDeleteSku,
  onApplyDown,
}: SkuMatrixTableProps) {
  const [showLogistics, setShowLogistics] = useState(false);

  return (
    <div className="space-y-4">
      {/* Controls row */}
      <div className="flex items-center justify-between gap-4 flex-wrap">
        {/* Apply-down shortcuts */}
        {skus.length > 1 && (
          <div className="flex items-center gap-2">
            <span className="text-xs text-muted-foreground">Apply first row to all:</span>
            <button
              type="button"
              onClick={() => onApplyDown("price")}
              className="flex items-center gap-1 px-2.5 py-1 rounded-md border border-muted-foreground/30 text-xs text-muted-foreground hover:border-primary hover:text-primary hover:bg-primary/5 transition-colors"
            >
              <ChevronsDown className="h-3 w-3" />
              Price
            </button>
            <button
              type="button"
              onClick={() => onApplyDown("stockQuantity")}
              className="flex items-center gap-1 px-2.5 py-1 rounded-md border border-muted-foreground/30 text-xs text-muted-foreground hover:border-primary hover:text-primary hover:bg-primary/5 transition-colors"
            >
              <ChevronsDown className="h-3 w-3" />
              Stock
            </button>
          </div>
        )}

        {/* Logistics toggle */}
        <label className="flex items-center gap-2 cursor-pointer select-none ml-auto">
          <Toggle checked={showLogistics} onChange={setShowLogistics} />
          <span className="text-xs text-muted-foreground">Show logistics fields</span>
        </label>
      </div>

      {/* Table */}
      <div className="overflow-x-auto rounded-lg border">
        <table className="w-full text-sm border-collapse">
          <thead>
            <tr className="border-b bg-gray-50/80">
              <th className="px-3 py-2.5 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wide whitespace-nowrap">
                Size <span className="text-destructive">*</span>
              </th>
              <th className="px-3 py-2.5 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wide whitespace-nowrap">
                Barcode <span className="text-destructive">*</span>
              </th>
              <th className="px-3 py-2.5 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wide whitespace-nowrap min-w-[110px]">
                Price (TJS)
              </th>
              <th className="px-3 py-2.5 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wide whitespace-nowrap min-w-[90px]">
                Stock
              </th>
              {showLogistics && (
                <>
                  <th className="px-3 py-2.5 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wide whitespace-nowrap">
                    Weight kg
                  </th>
                  <th className="px-3 py-2.5 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wide whitespace-nowrap">
                    W cm
                  </th>
                  <th className="px-3 py-2.5 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wide whitespace-nowrap">
                    H cm
                  </th>
                  <th className="px-3 py-2.5 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wide whitespace-nowrap">
                    D cm
                  </th>
                </>
              )}
              <th className="px-3 py-2.5 w-10" />
            </tr>
          </thead>
          <tbody className="divide-y">
            {skus.map((row) => {
              const sizeErr = submitted && errors.emptySize.has(row._key);
              const barcodeErr = submitted && errors.emptyBarcode.has(row._key);
              const rowHasError = sizeErr || barcodeErr;

              return (
                <tr
                  key={row._key}
                  className={cn(
                    "transition-colors",
                    rowHasError
                      ? "bg-destructive/5"
                      : "hover:bg-gray-50/80 bg-white"
                  )}
                >
                  <td className="px-3 py-2.5">
                    <Input
                      value={row.sizeLabel}
                      onChange={(e) =>
                        onUpdateSku(row._key, { sizeLabel: e.target.value })
                      }
                      placeholder="S, M, L, 42…"
                      className={cn(
                        "h-8 w-28",
                        sizeErr &&
                          "border-destructive focus-visible:ring-destructive"
                      )}
                    />
                    {sizeErr && (
                      <p className="text-[10px] text-destructive mt-0.5">Required</p>
                    )}
                  </td>

                  <td className="px-3 py-2.5">
                    <Input
                      value={row.barcode}
                      onChange={(e) =>
                        onUpdateSku(row._key, { barcode: e.target.value })
                      }
                      placeholder="EAN / UPC"
                      className={cn(
                        "h-8 w-36",
                        barcodeErr &&
                          "border-destructive focus-visible:ring-destructive"
                      )}
                    />
                    {barcodeErr && (
                      <p className="text-[10px] text-destructive mt-0.5">Required</p>
                    )}
                  </td>

                  <td className="px-3 py-2.5">
                    <Input
                      type="number"
                      min={0}
                      step={0.01}
                      value={row.price}
                      onChange={(e) =>
                        onUpdateSku(row._key, {
                          price: parseFloat(e.target.value) || 0,
                        })
                      }
                      className="h-8 w-28"
                    />
                  </td>

                  <td className="px-3 py-2.5">
                    <Input
                      type="number"
                      min={0}
                      step={1}
                      value={row.stockQuantity}
                      onChange={(e) =>
                        onUpdateSku(row._key, {
                          stockQuantity: parseInt(e.target.value) || 0,
                        })
                      }
                      className="h-8 w-24"
                    />
                  </td>

                  {showLogistics && (
                    <>
                      <td className="px-3 py-2.5">
                        <Input
                          type="number"
                          min={0}
                          step={0.01}
                          placeholder="—"
                          value={row.weightKg ?? ""}
                          onChange={(e) =>
                            onUpdateSku(row._key, {
                              weightKg: e.target.value
                                ? parseFloat(e.target.value)
                                : undefined,
                            })
                          }
                          className="h-8 w-20"
                        />
                      </td>
                      <td className="px-3 py-2.5">
                        <Input
                          type="number"
                          min={0}
                          placeholder="—"
                          value={row.widthCm ?? ""}
                          onChange={(e) =>
                            onUpdateSku(row._key, {
                              widthCm: e.target.value
                                ? parseFloat(e.target.value)
                                : undefined,
                            })
                          }
                          className="h-8 w-20"
                        />
                      </td>
                      <td className="px-3 py-2.5">
                        <Input
                          type="number"
                          min={0}
                          placeholder="—"
                          value={row.heightCm ?? ""}
                          onChange={(e) =>
                            onUpdateSku(row._key, {
                              heightCm: e.target.value
                                ? parseFloat(e.target.value)
                                : undefined,
                            })
                          }
                          className="h-8 w-20"
                        />
                      </td>
                      <td className="px-3 py-2.5">
                        <Input
                          type="number"
                          min={0}
                          placeholder="—"
                          value={row.depthCm ?? ""}
                          onChange={(e) =>
                            onUpdateSku(row._key, {
                              depthCm: e.target.value
                                ? parseFloat(e.target.value)
                                : undefined,
                            })
                          }
                          className="h-8 w-20"
                        />
                      </td>
                    </>
                  )}

                  <td className="px-3 py-2.5">
                    <button
                      type="button"
                      onClick={() => onDeleteSku(row._key)}
                      disabled={skus.length === 1}
                      className="h-8 w-8 flex items-center justify-center rounded-md text-muted-foreground hover:text-destructive hover:bg-destructive/10 transition-colors disabled:opacity-25 disabled:pointer-events-none"
                      aria-label="Delete row"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* Footer row */}
      <div className="flex items-center justify-between">
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={onAddSku}
          className="gap-1.5 text-muted-foreground hover:text-foreground"
        >
          <Plus className="h-3.5 w-3.5" />
          Add size row
        </Button>
        <span className="text-xs text-muted-foreground">
          {skus.length} SKU{skus.length !== 1 ? "s" : ""}
        </span>
      </div>
    </div>
  );
}
