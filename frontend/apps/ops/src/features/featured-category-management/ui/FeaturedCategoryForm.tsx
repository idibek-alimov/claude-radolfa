"use client";

import { useEffect, useRef, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import Image from "next/image";
import { AlertCircle, ImagePlus, Loader2, RefreshCw, X } from "lucide-react";

import {
  createFeaturedCategory,
  updateFeaturedCategory,
  uploadFeaturedCategoryImage,
  type FeaturedCategory,
  type FeaturedCategoryRequest,
} from "@/entities/featured-category";

import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@radolfa/shared/ui/dialog";
import { Button } from "@radolfa/shared/ui/button";
import { Input } from "@radolfa/shared/ui/input";
import { Label } from "@radolfa/shared/ui/label";
import { Switch } from "@radolfa/shared/ui/switch";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@radolfa/shared/ui/select";
import { cn, getErrorMessage } from "@radolfa/shared/lib";

import type { FlatCategory } from "../lib/flattenTree";

interface FeaturedCategoryFormProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** null = create mode; otherwise the entry being edited. */
  entry: FeaturedCategory | null;
  categories: FlatCategory[];
}

export function FeaturedCategoryForm({ open, onOpenChange, entry, categories }: FeaturedCategoryFormProps) {
  const qc = useQueryClient();
  const isEdit = entry !== null;

  const [categoryId, setCategoryId] = useState<number | "">("");
  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [title, setTitle] = useState("");
  const [subtitle, setSubtitle] = useState("");
  const [displayOrder, setDisplayOrder] = useState(0);
  const [active, setActive] = useState(true);
  const [touched, setTouched] = useState(false);

  // Reset form state whenever the dialog (re)opens — covers both
  // create (entry === null) and edit (entry populated).
  useEffect(() => {
    if (open) {
      setCategoryId(entry?.categoryId ?? "");
      setImageUrl(entry?.imageUrl ?? null);
      setTitle(entry?.title ?? "");
      setSubtitle(entry?.subtitle ?? "");
      setDisplayOrder(entry?.displayOrder ?? 0);
      setActive(entry?.active ?? true);
      setTouched(false);
    }
  }, [open, entry]);

  const mutation = useMutation({
    mutationFn: (body: FeaturedCategoryRequest) =>
      isEdit ? updateFeaturedCategory(entry.id, body) : createFeaturedCategory(body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["featured-categories"] });
      onOpenChange(false);
      toast.success(isEdit ? "Featured category updated" : "Featured category created");
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  function handleSubmit() {
    setTouched(true);
    if (categoryId === "") return;
    mutation.mutate({
      categoryId,
      imageUrl,
      title: title.trim() || null,
      subtitle: subtitle.trim() || null,
      displayOrder,
      active,
    });
  }

  const categoryError = touched && categoryId === "" ? "Pick a category" : null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>{isEdit ? "Edit Featured Category" : "New Featured Category"}</DialogTitle>
        </DialogHeader>

        <div className="grid grid-cols-[1fr_auto] gap-5">
          <div className="space-y-4">
            <div className="space-y-1.5">
              <Label>Category</Label>
              <Select
                value={categoryId === "" ? undefined : String(categoryId)}
                onValueChange={(v) => setCategoryId(Number(v))}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select a category" />
                </SelectTrigger>
                <SelectContent>
                  {categories.map((c) => (
                    <SelectItem key={c.id} value={String(c.id)}>
                      {"  ".repeat(c.depth)}
                      {c.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              {categoryError && <p className="text-destructive text-sm">{categoryError}</p>}
            </div>

            <div className="space-y-1.5">
              <Label>Title override (optional)</Label>
              <Input
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                maxLength={160}
                placeholder="Falls back to the category name"
              />
            </div>

            <div className="space-y-1.5">
              <Label>Subtitle (optional)</Label>
              <Input
                value={subtitle}
                onChange={(e) => setSubtitle(e.target.value)}
                maxLength={255}
                placeholder="A short merchandising line"
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label>Display order</Label>
                <Input
                  type="number"
                  min={0}
                  value={displayOrder}
                  onChange={(e) => setDisplayOrder(Math.max(0, Number(e.target.value) || 0))}
                />
              </div>
              <div className="space-y-1.5">
                <Label>Active</Label>
                <div className="flex items-center h-9 gap-2">
                  <Switch checked={active} onCheckedChange={setActive} />
                  <span className="text-sm text-muted-foreground">
                    {active ? "Visible on homepage" : "Hidden"}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <SpotlightImageZone imageUrl={imageUrl} onChange={setImageUrl} />
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button disabled={mutation.isPending} onClick={handleSubmit}>
            {mutation.isPending ? "Saving…" : isEdit ? "Save Changes" : "Create Entry"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

// ── SpotlightImageZone ──────────────────────────────────────────────
// Single-image upload zone — a stripped-down clone of the MediaZone in
// `features/product-creation/ui/steps/Step2Variants.tsx` (hidden input +
// dropzone + uploading/error states), minus `multiple` and the dnd-kit
// reorder grid: a spotlight tile needs exactly one optional image.

interface SpotlightImageZoneProps {
  imageUrl: string | null;
  onChange: (url: string | null) => void;
}

function SpotlightImageZone({ imageUrl, onChange }: SpotlightImageZoneProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [isDragOver, setIsDragOver] = useState(false);
  const [status, setStatus] = useState<"idle" | "uploading" | "error">("idle");

  async function processFile(file: File) {
    setStatus("uploading");
    try {
      const { url } = await uploadFeaturedCategoryImage(file);
      onChange(url);
      setStatus("idle");
    } catch {
      setStatus("error");
    }
  }

  function handleFileInput(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (file) processFile(file);
    e.target.value = "";
  }

  function handleDrop(e: React.DragEvent) {
    e.preventDefault();
    setIsDragOver(false);
    const file = Array.from(e.dataTransfer.files).find((f) => f.type.startsWith("image/"));
    if (file) processFile(file);
  }

  const dragHandlers = {
    onDragOver: (e: React.DragEvent) => {
      e.preventDefault();
      setIsDragOver(true);
    },
    onDragLeave: () => setIsDragOver(false),
    onDrop: handleDrop,
  };

  return (
    <div className="space-y-2 w-40 shrink-0">
      <Label>Spotlight image (optional)</Label>
      <input ref={inputRef} type="file" accept="image/*" className="hidden" onChange={handleFileInput} />

      {status === "error" && (
        <div className="flex items-start gap-1.5 rounded-lg bg-destructive/10 border border-destructive/20 px-2.5 py-2 text-xs text-destructive">
          <AlertCircle className="h-3.5 w-3.5 shrink-0 mt-0.5" />
          <span className="flex-1">Upload failed</span>
          <button
            type="button"
            onClick={() => setStatus("idle")}
            className="flex items-center gap-1 underline underline-offset-2 hover:opacity-70"
          >
            <RefreshCw className="h-3 w-3" />
          </button>
        </div>
      )}

      {imageUrl ? (
        <div className="relative aspect-[4/5] rounded-xl overflow-hidden border group">
          <Image src={imageUrl} alt="Spotlight preview" fill unoptimized className="object-cover" />
          <button
            type="button"
            onClick={() => onChange(null)}
            className="absolute top-1.5 right-1.5 h-6 w-6 rounded-full bg-black/60 text-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"
          >
            <X className="h-3.5 w-3.5" />
          </button>
        </div>
      ) : status === "uploading" ? (
        <div className="aspect-[4/5] rounded-xl border bg-muted/60 flex items-center justify-center animate-pulse">
          <Loader2 className="h-5 w-5 animate-spin text-muted-foreground/60" />
        </div>
      ) : (
        <div
          onClick={() => inputRef.current?.click()}
          {...dragHandlers}
          className={cn(
            "flex flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed p-4 cursor-pointer transition-all duration-200 aspect-[4/5]",
            isDragOver
              ? "border-primary bg-primary/5 scale-[0.99]"
              : "border-muted-foreground/20 hover:border-primary/40 hover:bg-gray-50/80"
          )}
        >
          <div
            className={cn(
              "h-9 w-9 rounded-xl flex items-center justify-center transition-all duration-200",
              isDragOver ? "bg-primary/10 scale-110" : "bg-muted"
            )}
          >
            <ImagePlus className={cn("h-4 w-4", isDragOver ? "text-primary" : "text-muted-foreground")} />
          </div>
          <p className="text-[11px] text-center text-muted-foreground leading-tight">
            Drop or click to upload
          </p>
        </div>
      )}
      <p className="text-[11px] text-muted-foreground leading-snug">
        When absent, the homepage tile uses a gradient fallback.
      </p>
    </div>
  );
}
