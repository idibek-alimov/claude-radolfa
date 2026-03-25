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
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/shared/ui/tabs";
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
  const [colorPickerOpen, setColorPickerOpen] = useState(false);
  const [variantToRemove, setVariantToRemove] = useState<number | null>(null);

  const { data: colors = [] } = useQuery({
    queryKey: ["colors"],
    queryFn: fetchColors,
  });

  const colorMap = Object.fromEntries(colors.map((c) => [c.id, c]));
  const usedColorIds = new Set(state.variants.map((v) => v.colorId));

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
    setColorPickerOpen(false);
  }

  function removeVariant(colorId: number) {
    update({ variants: state.variants.filter((v) => v.colorId !== colorId) });
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

  // ── removal confirmation data ──────────────────────────────────────

  const removingVariant = variantToRemove !== null
    ? state.variants.find((v) => v.colorId === variantToRemove)
    : null;
  const removingColor = variantToRemove !== null
    ? colorMap[variantToRemove]
    : null;

  // ── render ─────────────────────────────────────────────────────────

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between gap-4">
        <div>
          <h2 className="text-lg font-semibold">Variants &amp; Media</h2>
          <p className="text-sm text-muted-foreground mt-0.5">
            Add a tab for each color variant. Each tab holds images, visibility
            settings, and the SKU matrix.
          </p>
        </div>
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => setColorPickerOpen(true)}
          className="shrink-0 gap-1.5"
        >
          <Plus className="h-4 w-4" />
          Add Color Variant
        </Button>
      </div>

      {/* Empty state */}
      {state.variants.length === 0 && (
        <div className="rounded-lg border border-dashed p-12 text-center text-sm text-muted-foreground">
          No variants yet — click{" "}
          <span className="font-medium">Add Color Variant</span> to start.
        </div>
      )}

      {/* Tabs — one per variant */}
      {state.variants.length > 0 && (
        <Tabs defaultValue={String(state.variants[0].colorId)}>
          <TabsList className="flex-wrap h-auto gap-1 justify-start">
            {state.variants.map((variant) => {
              const color = colorMap[variant.colorId];
              const skuCount = variant.skus.length;
              return (
                <div key={variant.colorId} className="relative flex items-center">
                  <TabsTrigger
                    value={String(variant.colorId)}
                    className="gap-1.5 pr-7"
                  >
                    <span
                      className="h-3 w-3 rounded-full border border-black/10 shrink-0"
                      style={{ backgroundColor: color?.hexCode ?? "#e5e7eb" }}
                    />
                    {color?.displayName ?? color?.colorKey ?? `#${variant.colorId}`}
                    {skuCount > 0 && (
                      <span className="ml-1 text-xs text-muted-foreground">
                        ({skuCount})
                      </span>
                    )}
                  </TabsTrigger>
                  <button
                    type="button"
                    onClick={() => handleRemoveClick(variant.colorId)}
                    className="absolute right-1.5 h-4 w-4 flex items-center justify-center rounded-sm text-muted-foreground hover:text-destructive hover:bg-destructive/10 transition-colors"
                    aria-label={`Remove ${color?.displayName ?? "color"} variant`}
                  >
                    <X className="h-3 w-3" />
                  </button>
                </div>
              );
            })}
          </TabsList>

          {state.variants.map((variant) => (
            <TabsContent key={variant.colorId} value={String(variant.colorId)}>
              <VariantTabContent
                variant={variant}
                color={colorMap[variant.colorId]}
                submitted={submitted}
                errors={errors}
                onUpdateVariant={(patch) => updateVariant(variant.colorId, patch)}
                onUpdateSku={(key, patch) => updateSku(variant.colorId, key, patch)}
                onAddSku={() => addSku(variant.colorId)}
                onDeleteSku={(key) => deleteSku(variant.colorId, key)}
                onApplyDown={(field) => applyDown(variant.colorId, field)}
              />
            </TabsContent>
          ))}
        </Tabs>
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
                {removingColor?.displayName ?? removingColor?.colorKey ?? "this color"}
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

  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>Choose a color</DialogTitle>
        </DialogHeader>
        {available.length === 0 ? (
          <p className="text-sm text-muted-foreground py-4 text-center">
            All available colors have been added.
          </p>
        ) : (
          <div className="grid grid-cols-2 gap-2 pt-2">
            {available.map((color) => (
              <button
                key={color.id}
                type="button"
                onClick={() => onSelect(color.id)}
                className="flex items-center gap-2.5 rounded-md border px-3 py-2.5 text-sm hover:border-primary hover:bg-primary/5 transition-colors text-left"
              >
                <span
                  className="h-4 w-4 rounded-full border border-black/10 shrink-0"
                  style={{ backgroundColor: color.hexCode ?? "#e5e7eb" }}
                />
                <span className="truncate">
                  {color.displayName ?? color.colorKey}
                </span>
              </button>
            ))}
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}

// ── Toggle ────────────────────────────────────────────────────────────

interface ToggleProps {
  checked: boolean;
  onChange: (value: boolean) => void;
  disabled?: boolean;
}

function Toggle({ checked, onChange, disabled }: ToggleProps) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      onClick={() => !disabled && onChange(!checked)}
      disabled={disabled}
      className={cn(
        "relative inline-flex h-5 w-9 shrink-0 items-center rounded-full transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
        checked ? "bg-primary" : "bg-muted-foreground/30",
        disabled && "opacity-40 cursor-not-allowed"
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

// ── VariantTabContent ─────────────────────────────────────────────────

interface VariantTabContentProps {
  variant: VariantDraft;
  color: Color | undefined;
  submitted: boolean;
  errors: Step2Errors;
  onUpdateVariant: (patch: Partial<VariantDraft>) => void;
  onUpdateSku: (key: string, patch: Partial<SkuRow>) => void;
  onAddSku: () => void;
  onDeleteSku: (key: string) => void;
  onApplyDown: (field: "price" | "stockQuantity") => void;
}

function VariantTabContent({
  variant,
  submitted,
  errors,
  onUpdateVariant,
  onUpdateSku,
  onAddSku,
  onDeleteSku,
  onApplyDown,
}: VariantTabContentProps) {
  return (
    <div className="space-y-8 pt-4">
      {/* 1. Media zone */}
      <div className="space-y-3">
        <p className="text-sm font-semibold">Images</p>
        <MediaZone
          images={variant.images}
          onUploaded={(url) =>
            onUpdateVariant({ images: [...variant.images, url] })
          }
          onDelete={(url) =>
            onUpdateVariant({ images: variant.images.filter((u) => u !== url) })
          }
        />
      </div>

      {/* 3. SKU matrix */}
      <div className="space-y-3">
        <p className="text-sm font-semibold">SKU Matrix</p>
        <SkuMatrixTable
          skus={variant.skus}
          submitted={submitted}
          errors={errors}
          onUpdateSku={onUpdateSku}
          onAddSku={onAddSku}
          onDeleteSku={onDeleteSku}
          onApplyDown={onApplyDown}
        />
      </div>
    </div>
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

  return (
    <div className="space-y-4">
      <input
        ref={inputRef}
        type="file"
        accept="image/*"
        multiple
        className="hidden"
        onChange={handleFileInput}
      />

      {/* Large dropzone — shown only when there are no images yet */}
      {!hasImages && (
        <div
          onClick={() => inputRef.current?.click()}
          onDragOver={(e) => {
            e.preventDefault();
            setIsDragOver(true);
          }}
          onDragLeave={() => setIsDragOver(false)}
          onDrop={handleDrop}
          className={cn(
            "flex flex-col items-center justify-center gap-2 rounded-lg border-2 border-dashed p-8 cursor-pointer transition-colors",
            isDragOver
              ? "border-primary bg-primary/5"
              : "border-muted-foreground/25 hover:border-primary/50 hover:bg-muted/30"
          )}
        >
          <ImagePlus className="h-8 w-8 text-muted-foreground" />
          <div className="text-center">
            <p className="text-sm font-medium">Drop images here or click to browse</p>
            <p className="text-xs text-muted-foreground mt-0.5">PNG, JPG, WebP</p>
          </div>
        </div>
      )}

      {errorIds.length > 0 && (
        <div className="flex items-center gap-2 rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>
            {errorIds.length}{" "}
            {errorIds.length === 1 ? "image" : "images"} failed to upload.
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
            className="ml-auto flex items-center gap-1 underline underline-offset-2 hover:opacity-70"
          >
            <RefreshCw className="h-3 w-3" />
            Dismiss
          </button>
        </div>
      )}

      {/* Image grid — shown once at least one image exists */}
      {hasImages && (
        <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-5 gap-3">
          {images.map((url, index) => (
            <div key={url} className="group relative aspect-square">
              <Image
                src={url}
                alt="Product image"
                fill
                className="rounded-md object-cover border"
                unoptimized
              />
              {index === 0 && (
                <span className="absolute bottom-0 left-0 right-0 bg-amber-500/90 text-white text-[10px] font-semibold text-center py-0.5 rounded-b-md pointer-events-none">
                  Main
                </span>
              )}
              <button
                type="button"
                onClick={() => onDelete(url)}
                className="absolute -top-1.5 -right-1.5 h-5 w-5 rounded-full bg-destructive text-destructive-foreground flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity shadow-sm"
              >
                <X className="h-3 w-3" />
              </button>
            </div>
          ))}
          {Array.from({ length: uploadingCount }).map((_, i) => (
            <div
              key={`uploading-${i}`}
              className="aspect-square rounded-md border bg-muted flex items-center justify-center"
            >
              <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
            </div>
          ))}
          {/* Compact add-more tile */}
          <div
            onClick={() => inputRef.current?.click()}
            onDragOver={(e) => {
              e.preventDefault();
              setIsDragOver(true);
            }}
            onDragLeave={() => setIsDragOver(false)}
            onDrop={handleDrop}
            className={cn(
              "aspect-square rounded-md border-2 border-dashed flex items-center justify-center cursor-pointer transition-colors",
              isDragOver
                ? "border-primary bg-primary/5"
                : "border-muted-foreground/25 hover:border-primary/50 hover:bg-muted/30"
            )}
          >
            <ImagePlus className="h-5 w-5 text-muted-foreground" />
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
    <div className="space-y-3">
      <div className="flex items-center gap-2.5">
        <Toggle checked={showLogistics} onChange={setShowLogistics} />
        <span className="text-sm text-muted-foreground">
          Logistics fields (weight, dimensions)
        </span>
      </div>

      <div className="overflow-x-auto rounded-lg border">
        <table className="w-full text-sm border-collapse">
          <thead>
            <tr className="border-b bg-muted/50">
              <th className="px-3 py-2.5 text-left font-medium text-muted-foreground whitespace-nowrap">
                Size Label <span className="text-destructive">*</span>
              </th>
              <th className="px-3 py-2.5 text-left font-medium text-muted-foreground whitespace-nowrap">
                Barcode <span className="text-destructive">*</span>
              </th>
              <th className="px-3 py-2.5 text-left font-medium text-muted-foreground whitespace-nowrap min-w-[120px]">
                Price (TJS)
              </th>
              <th className="px-3 py-2.5 text-left font-medium text-muted-foreground whitespace-nowrap min-w-[100px]">
                Stock
              </th>
              {showLogistics && (
                <>
                  <th className="px-3 py-2.5 text-left font-medium text-muted-foreground whitespace-nowrap">
                    Weight (kg)
                  </th>
                  <th className="px-3 py-2.5 text-left font-medium text-muted-foreground whitespace-nowrap">
                    W (cm)
                  </th>
                  <th className="px-3 py-2.5 text-left font-medium text-muted-foreground whitespace-nowrap">
                    H (cm)
                  </th>
                  <th className="px-3 py-2.5 text-left font-medium text-muted-foreground whitespace-nowrap">
                    D (cm)
                  </th>
                </>
              )}
              <th className="px-3 py-2.5 w-10" />
            </tr>
          </thead>
          <tbody>
            {/* Apply-down bar */}
            {skus.length > 1 && (
              <tr className="border-b bg-muted/20">
                <td colSpan={showLogistics ? 9 : 5} className="px-3 py-1">
                  <div className="flex items-center gap-3 text-xs text-muted-foreground">
                    <span>Apply first row value to all rows:</span>
                    <button
                      type="button"
                      onClick={() => onApplyDown("price")}
                      className="underline underline-offset-2 hover:text-foreground"
                    >
                      ⬇ Price
                    </button>
                    <button
                      type="button"
                      onClick={() => onApplyDown("stockQuantity")}
                      className="underline underline-offset-2 hover:text-foreground"
                    >
                      ⬇ Stock
                    </button>
                  </div>
                </td>
              </tr>
            )}

            {skus.map((row) => {
              const sizeErr = submitted && errors.emptySize.has(row._key);
              const barcodeErr = submitted && errors.emptyBarcode.has(row._key);

              return (
                <tr
                  key={row._key}
                  className="border-b last:border-b-0 hover:bg-muted/30 transition-colors bg-background"
                >
                  <td className="px-3 py-2">
                    <Input
                      value={row.sizeLabel}
                      onChange={(e) =>
                        onUpdateSku(row._key, { sizeLabel: e.target.value })
                      }
                      placeholder="S, M, L, 42, ONE_SIZE…"
                      className={cn(
                        "h-8 w-32",
                        sizeErr &&
                          "border-destructive focus-visible:ring-destructive"
                      )}
                    />
                    {sizeErr && (
                      <p className="text-xs text-destructive mt-0.5">Required</p>
                    )}
                  </td>

                  <td className="px-3 py-2">
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
                      <p className="text-xs text-destructive mt-0.5">Required</p>
                    )}
                  </td>

                  <td className="px-3 py-2">
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

                  <td className="px-3 py-2">
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
                      <td className="px-3 py-2">
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
                      <td className="px-3 py-2">
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
                      <td className="px-3 py-2">
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
                      <td className="px-3 py-2">
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

                  <td className="px-3 py-2">
                    <button
                      type="button"
                      onClick={() => onDeleteSku(row._key)}
                      disabled={skus.length === 1}
                      className="h-8 w-8 flex items-center justify-center rounded-md text-muted-foreground hover:text-destructive hover:bg-destructive/10 transition-colors disabled:opacity-30 disabled:pointer-events-none"
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

      <p className="text-xs text-muted-foreground">
        {skus.length} SKU{skus.length !== 1 ? "s" : ""} in this variant
      </p>
    </div>
  );
}
