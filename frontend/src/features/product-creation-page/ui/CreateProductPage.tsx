"use client";

import { useState, useRef } from "react";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import { ChevronLeft, ImagePlus, X, Loader2, Plus, Trash2 } from "lucide-react";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { createProduct } from "@/entities/product/api/admin";
import { uploadListingImage, fetchCategoryTree } from "@/entities/product/api";
import { fetchColors } from "@/entities/color";
import { getErrorMessage } from "@/shared/lib/utils";
import type { CategoryTree } from "@/entities/product/model/types";

// ── Helpers ──────────────────────────────────────────────────────────
function flattenTree(
  nodes: CategoryTree[],
  depth = 0
): { id: number; name: string; depth: number }[] {
  return nodes.flatMap((node) => [
    { id: node.id, name: node.name, depth },
    ...flattenTree(node.children, depth + 1),
  ]);
}

// ── Component ────────────────────────────────────────────────────────
export function CreateProductPage() {
  const router = useRouter();

  // ── Form state ───────────────────────────────────────────────────
  const [name, setName] = useState("");
  const [categoryId, setCategoryId] = useState<number | null>(null);
  const [colorId, setColorId] = useState<number | null>(null);
  const [webDescription, setWebDescription] = useState("");
  const [skus, setSkus] = useState<
    Array<{ sizeLabel: string; price: string; stockQuantity: string }>
  >([{ sizeLabel: "", price: "", stockQuantity: "" }]);
  const [attributes, setAttributes] = useState<
    Array<{ key: string; value: string }>
  >([]);
  const [files, setFiles] = useState<File[]>([]);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [submitPhase, setSubmitPhase] = useState<
    "idle" | "creating" | "uploading"
  >("idle");

  const fileInputRef = useRef<HTMLInputElement>(null);
  const dragItem = useRef<number | null>(null);
  const dragOverItem = useRef<number | null>(null);

  // ── Data fetching ────────────────────────────────────────────────
  const { data: categories } = useQuery({
    queryKey: ["categories"],
    queryFn: fetchCategoryTree,
  });

  const { data: colors } = useQuery({
    queryKey: ["colors"],
    queryFn: fetchColors,
  });

  const flatCategories = categories ? flattenTree(categories) : [];

  // ── SKU helpers ──────────────────────────────────────────────────
  function addSkuRow() {
    setSkus((prev) => [...prev, { sizeLabel: "", price: "", stockQuantity: "" }]);
  }

  function removeSkuRow(index: number) {
    setSkus((prev) => prev.filter((_, i) => i !== index));
  }

  function updateSku(
    index: number,
    field: "sizeLabel" | "price" | "stockQuantity",
    value: string
  ) {
    setSkus((prev) =>
      prev.map((row, i) => (i === index ? { ...row, [field]: value } : row))
    );
  }

  // ── Attribute helpers ────────────────────────────────────────────
  function addAttributeRow() {
    setAttributes((prev) => [...prev, { key: "", value: "" }]);
  }

  function removeAttributeRow(index: number) {
    setAttributes((prev) => prev.filter((_, i) => i !== index));
  }

  function updateAttribute(
    index: number,
    field: "key" | "value",
    value: string
  ) {
    setAttributes((prev) =>
      prev.map((row, i) => (i === index ? { ...row, [field]: value } : row))
    );
  }

  // ── Validation ───────────────────────────────────────────────────
  function validate(): boolean {
    const errs: Record<string, string> = {};
    if (!name.trim()) errs.name = "Product name is required.";
    if (categoryId === null) errs.categoryId = "Category is required.";
    if (colorId === null) errs.colorId = "Color is required.";
    if (skus.length === 0) errs.skus = "At least one SKU is required.";
    for (let i = 0; i < skus.length; i++) {
      if (!skus[i].sizeLabel.trim()) errs[`sku_size_${i}`] = "Required.";
      const price = parseFloat(skus[i].price);
      if (isNaN(price) || price < 0) errs[`sku_price_${i}`] = "Must be 0 or greater.";
      const stock = parseInt(skus[i].stockQuantity, 10);
      if (isNaN(stock) || stock < 0) errs[`sku_stock_${i}`] = "Must be 0 or greater.";
    }
    setErrors(errs);
    return Object.keys(errs).length === 0;
  }

  // ── Submit handler ───────────────────────────────────────────────
  async function handleSubmit() {
    if (!validate()) return;
    setSubmitPhase("creating");
    let slug: string;
    try {
      const result = await createProduct({
        name: name.trim(),
        categoryId: categoryId!,
        colorId: colorId!,
        webDescription: webDescription || undefined,
        skus: skus.map((s) => ({
          sizeLabel: s.sizeLabel,
          price: parseFloat(s.price),
          stockQuantity: parseInt(s.stockQuantity, 10),
        })),
        attributes: attributes.filter((a) => a.key.trim() && a.value.trim()).length > 0
          ? attributes
              .filter((a) => a.key.trim() && a.value.trim())
              .map((a) => ({ key: a.key.trim(), value: a.value.trim() }))
          : undefined,
      });
      slug = result.slug;
    } catch (err) {
      toast.error(getErrorMessage(err));
      setSubmitPhase("idle");
      return;
    }

    if (files.length > 0) {
      setSubmitPhase("uploading");
      try {
        for (const file of files) {
          await uploadListingImage(slug, file);
        }
      } catch (err) {
        toast.error("Images could not be uploaded. You can add them from the edit page.");
      }
    }

    toast.success("Product created.");
    setFiles([]);
    router.push(`/manage/products/${slug}/edit`);
  }

  // ── Render ───────────────────────────────────────────────────────
  return (
    <div className="min-h-screen bg-muted/30 py-10">
      <div className="max-w-[1600px] mx-auto px-4 sm:px-6 lg:px-8">
        {/* Back link */}
        <button
          onClick={() => router.push("/manage")}
          className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors mb-4"
        >
          <ChevronLeft className="h-4 w-4" />
          Back to Products
        </button>

        {/* Page heading */}
        <h1 className="text-2xl font-semibold text-foreground">Create Product</h1>
        <p className="text-sm text-muted-foreground mt-1">
          Fill in the details below to add a new product to the catalog.
        </p>

        {/* Two-column grid */}
        <div className="grid grid-cols-1 lg:grid-cols-[1fr_auto] gap-8 mt-8">
          {/* ── Left column — Basic Info + Description ── */}
          <div className="bg-card rounded-xl border shadow-sm p-6 space-y-4">
            <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              Basic Info
            </p>

            {/* Name */}
            <div className="space-y-1">
              <label className="text-sm font-semibold">Product Name</label>
              <Input
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Classic Linen Shirt"
              />
              {errors.name && (
                <p className="text-xs text-destructive">{errors.name}</p>
              )}
            </div>

            {/* Category + Color */}
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="text-sm font-semibold">Category</label>
                <select
                  value={categoryId ?? ""}
                  onChange={(e) =>
                    setCategoryId(e.target.value ? Number(e.target.value) : null)
                  }
                  className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm focus:outline-none focus:ring-1 focus:ring-ring"
                >
                  <option value="">Select category</option>
                  {flatCategories.map((c) => (
                    <option key={c.id} value={c.id}>
                      {"  ".repeat(c.depth)}
                      {c.name}
                    </option>
                  ))}
                </select>
                {errors.categoryId && (
                  <p className="text-xs text-destructive">{errors.categoryId}</p>
                )}
              </div>

              <div className="space-y-1">
                <label className="text-sm font-semibold">Color</label>
                <select
                  value={colorId ?? ""}
                  onChange={(e) =>
                    setColorId(e.target.value ? Number(e.target.value) : null)
                  }
                  className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm focus:outline-none focus:ring-1 focus:ring-ring"
                >
                  <option value="">Select color</option>
                  {colors?.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.displayName ?? c.colorKey}
                    </option>
                  ))}
                </select>
                {errors.colorId && (
                  <p className="text-xs text-destructive">{errors.colorId}</p>
                )}
              </div>
            </div>

            {/* Description */}
            <div className="border-t pt-4 space-y-2">
              <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Description
              </p>
              <textarea
                value={webDescription}
                onChange={(e) => setWebDescription(e.target.value)}
                maxLength={5000}
                placeholder="Product description..."
                rows={5}
                className="flex w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm placeholder:text-muted-foreground focus:outline-none focus:ring-1 focus:ring-ring resize-y min-h-[120px]"
              />
              <p className={`text-xs text-right ${webDescription.length >= 4500 ? "text-destructive" : "text-muted-foreground"}`}>
                {webDescription.length} / 5000
              </p>
            </div>

            {/* Attributes */}
            <div className="border-t pt-4 space-y-2">
              <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Attributes
              </p>

              {attributes.map((attr, i) => (
                <div key={i} className="flex gap-2 items-center">
                  <Input
                    value={attr.key}
                    onChange={(e) => updateAttribute(i, "key", e.target.value)}
                    placeholder="e.g. Material"
                    className="h-8 text-sm flex-1"
                  />
                  <Input
                    value={attr.value}
                    onChange={(e) => updateAttribute(i, "value", e.target.value)}
                    placeholder="e.g. Cotton"
                    className="h-8 text-sm flex-1"
                  />
                  <button
                    type="button"
                    aria-label="Remove attribute"
                    onClick={() => removeAttributeRow(i)}
                    className="text-muted-foreground hover:text-destructive transition-colors"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              ))}

              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={addAttributeRow}
              >
                <Plus className="h-4 w-4 mr-1" />
                Add Attribute
              </Button>
            </div>
          </div>

          {/* ── Right column — Images + SKUs ── */}
          <div className="space-y-6 w-full lg:w-[420px]">
            {/* Image card */}
            <div className="bg-card rounded-xl border shadow-sm p-6 space-y-4">
              <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                Images
              </p>

              <input
                ref={fileInputRef}
                type="file"
                multiple
                accept="image/*"
                className="hidden"
                onChange={(e) =>
                  setFiles((prev) => [
                    ...prev,
                    ...Array.from(e.target.files ?? []),
                  ])
                }
              />

              {files.length === 0 ? (
                <div
                  className="border-2 border-dashed rounded-lg p-8 text-center cursor-pointer hover:bg-muted/30 transition-colors"
                  onClick={() => fileInputRef.current?.click()}
                >
                  <ImagePlus className="h-8 w-8 mx-auto text-muted-foreground mb-2" />
                  <p className="text-sm font-medium text-muted-foreground">
                    Add Images (optional)
                  </p>
                  <p className="text-xs text-muted-foreground mt-1">
                    Click to browse — drag to reorder, first = primary
                  </p>
                </div>
              ) : (
                <div className="flex flex-wrap gap-2">
                  {files.map((file, idx) => (
                    <div
                      key={idx}
                      className="relative h-16 w-16 cursor-grab active:cursor-grabbing"
                      draggable
                      onDragStart={() => { dragItem.current = idx; }}
                      onDragOver={(e) => { e.preventDefault(); dragOverItem.current = idx; }}
                      onDragEnd={() => {
                        if (dragItem.current !== null && dragOverItem.current !== null && dragItem.current !== dragOverItem.current) {
                          setFiles((prev) => {
                            const updated = [...prev];
                            const [dragged] = updated.splice(dragItem.current!, 1);
                            updated.splice(dragOverItem.current!, 0, dragged);
                            return updated;
                          });
                        }
                        dragItem.current = null;
                        dragOverItem.current = null;
                      }}
                    >
                      <img
                        src={URL.createObjectURL(file)}
                        alt={file.name}
                        className="h-16 w-16 rounded-md object-cover"
                      />
                      {idx === 0 && (
                        <span className="absolute bottom-0.5 left-0.5 z-10 rounded bg-black/60 px-1 py-0.5 text-[9px] font-medium text-white">
                          Primary
                        </span>
                      )}
                      <button
                        type="button"
                        aria-label="Remove image"
                        onClick={() =>
                          setFiles((f) => f.filter((_, i) => i !== idx))
                        }
                        className="absolute -top-1 -right-1 h-4 w-4 flex items-center justify-center text-white bg-black/50 rounded-full"
                      >
                        <X className="h-3 w-3" />
                      </button>
                    </div>
                  ))}
                </div>
              )}

              <Button
                type="button"
                variant="outline"
                onClick={() => fileInputRef.current?.click()}
              >
                <ImagePlus className="h-4 w-4 mr-2" />
                Add Images
              </Button>
            </div>

            {/* SKU card */}
            <div className="bg-card rounded-xl border shadow-sm p-6 space-y-4">
              <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                SKUs
              </p>

              {errors.skus && (
                <p className="text-xs text-destructive">{errors.skus}</p>
              )}

              <div className="rounded-md border text-sm overflow-hidden">
                <table className="w-full">
                  <thead>
                    <tr className="border-b bg-muted/50">
                      <th className="text-left px-3 py-2 text-xs font-medium text-muted-foreground">
                        Size Label
                      </th>
                      <th className="text-left px-3 py-2 text-xs font-medium text-muted-foreground">
                        Price (TJS)
                      </th>
                      <th className="text-left px-3 py-2 text-xs font-medium text-muted-foreground">
                        Stock
                      </th>
                      <th className="w-8" />
                    </tr>
                  </thead>
                  <tbody>
                    {skus.map((row, i) => (
                      <tr key={i} className="border-b last:border-0">
                        <td className="px-2 py-1.5">
                          <Input
                            value={row.sizeLabel}
                            onChange={(e) =>
                              updateSku(i, "sizeLabel", e.target.value)
                            }
                            placeholder="S / M / 42..."
                            className={`h-7 text-sm ${
                              errors[`sku_size_${i}`] ? "border-destructive" : ""
                            }`}
                          />
                        </td>
                        <td className="px-2 py-1.5">
                          <Input
                            type="number"
                            min={0}
                            step={0.01}
                            value={row.price}
                            onChange={(e) => updateSku(i, "price", e.target.value)}
                            className={`h-7 text-sm ${
                              errors[`sku_price_${i}`] ? "border-destructive" : ""
                            }`}
                          />
                        </td>
                        <td className="px-2 py-1.5">
                          <Input
                            type="number"
                            min={0}
                            value={row.stockQuantity}
                            onChange={(e) =>
                              updateSku(i, "stockQuantity", e.target.value)
                            }
                            className={`h-7 text-sm ${
                              errors[`sku_stock_${i}`] ? "border-destructive" : ""
                            }`}
                          />
                        </td>
                        <td className="px-1 py-1.5 text-center">
                          {skus.length > 1 && (
                            <button
                              type="button"
                              aria-label="Remove SKU"
                              onClick={() => removeSkuRow(i)}
                              className="text-muted-foreground hover:text-destructive transition-colors"
                            >
                              <Trash2 className="h-4 w-4" />
                            </button>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={addSkuRow}
              >
                <Plus className="h-4 w-4 mr-1" />
                Add Size
              </Button>
            </div>
          </div>
        </div>

        {/* Form action bar */}
        <div className="flex justify-end gap-3 pt-4">
          <Button
            type="button"
            variant="outline"
            onClick={() => router.push("/manage")}
          >
            Back to Products
          </Button>
          <Button
            type="submit"
            disabled={submitPhase !== "idle"}
            onClick={handleSubmit}
          >
            {(submitPhase === "creating" || submitPhase === "uploading") && (
              <Loader2 className="animate-spin h-4 w-4 mr-1" />
            )}
            {submitPhase === "creating"
              ? "Creating..."
              : submitPhase === "uploading"
              ? "Uploading images..."
              : "Create Product"}
          </Button>
        </div>
      </div>
    </div>
  );
}

export default CreateProductPage;
