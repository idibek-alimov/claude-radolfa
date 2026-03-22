"use client";

import { useRef, useState, useCallback } from "react";
import Image from "next/image";
import { useQuery } from "@tanstack/react-query";
import { ImagePlus, X, Loader2, AlertCircle, RefreshCw } from "lucide-react";
import { fetchColors } from "@/entities/color";
import { uploadProductImage } from "../../api/imageUpload";
import type { WizardState } from "../../model/types";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/shared/ui/tabs";
import { cn } from "@/shared/lib/utils";

interface Props {
  state: WizardState;
  update: (patch: Partial<WizardState>) => void;
}

// Per-file upload status keyed by a client-side temp id
type UploadStatus = "uploading" | "error";

export function Step2Media({ state, update }: Props) {
  const { data: colors } = useQuery({
    queryKey: ["colors"],
    queryFn: fetchColors,
  });

  const selectedColors = (colors ?? []).filter((c) =>
    state.colorIds.includes(c.id)
  );

  if (selectedColors.length === 0) {
    return (
      <div className="space-y-6">
        <StepHeader />
        <div className="rounded-lg border border-dashed p-12 text-center text-muted-foreground text-sm">
          No colors selected — go back to Step 1 and select at least one color.
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <StepHeader />
      <Tabs defaultValue={String(selectedColors[0].id)}>
        <TabsList className="flex-wrap h-auto gap-1 justify-start">
          {selectedColors.map((color) => {
            const count = (state.imagesByColorId[color.id] ?? []).length;
            return (
              <TabsTrigger
                key={color.id}
                value={String(color.id)}
                className="gap-1.5"
              >
                <span
                  className="h-3 w-3 rounded-full border border-black/10 shrink-0"
                  style={{ backgroundColor: color.hexCode ?? "#e5e7eb" }}
                />
                {color.displayName ?? color.colorKey}
                {count > 0 && (
                  <span className="ml-1 text-xs text-muted-foreground">
                    ({count})
                  </span>
                )}
              </TabsTrigger>
            );
          })}
        </TabsList>

        {selectedColors.map((color) => (
          <TabsContent key={color.id} value={String(color.id)}>
            <ColorUploadZone
              colorId={color.id}
              colorName={color.displayName ?? color.colorKey}
              urls={state.imagesByColorId[color.id] ?? []}
              onUploaded={(url) => {
                const prev = state.imagesByColorId[color.id] ?? [];
                update({
                  imagesByColorId: {
                    ...state.imagesByColorId,
                    [color.id]: [...prev, url],
                  },
                });
              }}
              onDelete={(url) => {
                const prev = state.imagesByColorId[color.id] ?? [];
                update({
                  imagesByColorId: {
                    ...state.imagesByColorId,
                    [color.id]: prev.filter((u) => u !== url),
                  },
                });
              }}
            />
          </TabsContent>
        ))}
      </Tabs>
    </div>
  );
}

// ── StepHeader ───────────────────────────────────────────────────────

function StepHeader() {
  return (
    <div>
      <h2 className="text-lg font-semibold">Media Upload</h2>
      <p className="text-sm text-muted-foreground mt-0.5">
        Upload images for each color variant. Images are optional — you can add
        them later via the product edit page.
      </p>
    </div>
  );
}

// ── ColorUploadZone ──────────────────────────────────────────────────

interface ColorUploadZoneProps {
  colorId: number;
  colorName: string;
  urls: string[];
  onUploaded: (url: string) => void;
  onDelete: (url: string) => void;
}

function ColorUploadZone({
  colorName,
  urls,
  onUploaded,
  onDelete,
}: ColorUploadZoneProps) {
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
      // Upload sequentially to avoid hammering the server
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
    // Reset so same file can be re-selected after error
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

  return (
    <div className="space-y-4 pt-2">
      {/* Drop zone */}
      <div
        onClick={() => inputRef.current?.click()}
        onDragOver={(e) => { e.preventDefault(); setIsDragOver(true); }}
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
          <p className="text-sm font-medium">
            Drop images here or click to browse
          </p>
          <p className="text-xs text-muted-foreground mt-0.5">
            PNG, JPG, WebP — for{" "}
            <span className="font-medium">{colorName}</span>
          </p>
        </div>
        <input
          ref={inputRef}
          type="file"
          accept="image/*"
          multiple
          className="hidden"
          onChange={handleFileInput}
        />
      </div>

      {/* Error summary */}
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

      {/* Thumbnail grid */}
      {(urls.length > 0 || uploadingCount > 0) && (
        <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-5 gap-3">
          {/* Uploaded thumbnails */}
          {urls.map((url) => (
            <div key={url} className="group relative aspect-square">
              <Image
                src={url}
                alt="Product image"
                fill
                className="rounded-md object-cover border"
                unoptimized
              />
              <button
                type="button"
                onClick={() => onDelete(url)}
                className="absolute -top-1.5 -right-1.5 h-5 w-5 rounded-full bg-destructive text-destructive-foreground flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity shadow-sm"
              >
                <X className="h-3 w-3" />
              </button>
            </div>
          ))}

          {/* Uploading placeholders */}
          {Array.from({ length: uploadingCount }).map((_, i) => (
            <div
              key={`uploading-${i}`}
              className="aspect-square rounded-md border bg-muted flex items-center justify-center"
            >
              <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
