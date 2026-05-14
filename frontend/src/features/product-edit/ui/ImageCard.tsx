"use client";

import { useRef, useState, useCallback, useEffect } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  X,
  ImagePlus,
  Loader2,
  AlertCircle,
  RefreshCw,
  Star,
} from "lucide-react";
import Image from "next/image";
import {
  DndContext,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
} from "@dnd-kit/core";
import {
  arrayMove,
  SortableContext,
  sortableKeyboardCoordinates,
  rectSortingStrategy,
  useSortable,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { uploadListingImage, removeListingImage } from "@/entities/product/api";
import { reorderVariantImages } from "@/entities/product/api/admin";
import { getErrorMessage } from "@/shared/lib";
import { cn } from "@/shared/lib/utils";
import type { ProductCardImage } from "@/entities/product/model/types";

interface Props {
  slug: string;
  variantId: number;
  productBaseId: number;
  images: ProductCardImage[];
}

// ── SortableImageCard ──────────────────────────────────────────────────

interface SortableImageCardProps {
  image: ProductCardImage;
  index: number;
  onDelete: (url: string) => void;
  isDeleting: boolean;
}

function SortableImageCard({ image, index, onDelete, isDeleting }: SortableImageCardProps) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: image.id.toString() });

  return (
    <div
      ref={setNodeRef}
      style={{ transform: CSS.Transform.toString(transform), transition }}
      className={cn(
        "aspect-square relative rounded-xl overflow-hidden border bg-gray-50 group select-none",
        isDragging
          ? "opacity-40 ring-2 ring-primary/50 shadow-2xl z-10"
          : "shadow-sm hover:shadow-md transition-shadow duration-150"
      )}
    >
      <Image
        src={image.url}
        alt={index === 0 ? "Main product image" : "Product image"}
        fill
        className="object-cover"
        unoptimized
        draggable={false}
      />

      {/* Drag surface */}
      <div
        {...attributes}
        {...listeners}
        className="absolute inset-0 cursor-grab active:cursor-grabbing"
        style={{ touchAction: "none" }}
      />

      {/* Main badge */}
      {index === 0 && (
        <div className="absolute top-1.5 left-1.5 z-10 flex items-center gap-0.5 bg-white/90 backdrop-blur-sm rounded-full px-1.5 py-0.5 shadow-sm pointer-events-none">
          <Star className="h-3 w-3 text-amber-500 fill-amber-500" />
          <span className="text-[10px] font-semibold text-amber-600">Main</span>
        </div>
      )}

      {/* Delete button */}
      <button
        type="button"
        onClick={() => onDelete(image.url)}
        disabled={isDeleting}
        className="absolute top-1.5 right-1.5 z-10 h-6 w-6 rounded-full bg-black/60 text-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity hover:bg-black/80 disabled:cursor-not-allowed"
        aria-label="Remove image"
      >
        <X className="h-3 w-3" />
      </button>
    </div>
  );
}

// ── MediaZone ──────────────────────────────────────────────────────────

type UploadStatus = "uploading" | "error";

interface MediaZoneProps {
  images: ProductCardImage[];
  onUploaded: (url: string) => void;
  onDelete: (url: string) => void;
  onReorder: (images: ProductCardImage[]) => void;
  slug: string;
  isDeletingUrl: string | null;
}

function MediaZone({ images, onUploaded, onDelete, onReorder, slug, isDeletingUrl }: MediaZoneProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [uploadStatuses, setUploadStatuses] = useState<Record<string, UploadStatus>>({});
  const [isDragOver, setIsDragOver] = useState(false);

  const onUploadedRef = useRef(onUploaded);
  onUploadedRef.current = onUploaded;

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 8 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates })
  );

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
          const result = await uploadListingImage(slug, file);
          const newUrl = result.images[result.images.length - 1];
          if (newUrl) {
            onUploadedRef.current(newUrl);
          }
          setStatus(tempId, null);
        } catch {
          setStatus(tempId, "error");
        }
      }
    },
    [slug]
  );

  function handleFileInput(e: React.ChangeEvent<HTMLInputElement>) {
    const files = Array.from(e.target.files ?? []);
    if (files.length) processFiles(files);
    e.target.value = "";
  }

  function handleFileDrop(e: React.DragEvent) {
    e.preventDefault();
    setIsDragOver(false);
    const files = Array.from(e.dataTransfer.files).filter((f) =>
      f.type.startsWith("image/")
    );
    if (files.length) processFiles(files);
  }

  function handleDragEnd(event: DragEndEvent) {
    const { active, over } = event;
    if (over && active.id !== over.id) {
      const oldIndex = images.findIndex((img) => img.id.toString() === active.id);
      const newIndex = images.findIndex((img) => img.id.toString() === over.id);
      if (oldIndex !== -1 && newIndex !== -1) {
        onReorder(arrayMove(images, oldIndex, newIndex));
      }
    }
  }

  const uploadingCount = Object.values(uploadStatuses).filter(
    (s) => s === "uploading"
  ).length;
  const errorIds = Object.entries(uploadStatuses)
    .filter(([, s]) => s === "error")
    .map(([id]) => id);

  const hasImages = images.length > 0 || uploadingCount > 0;

  const fileDragHandlers = {
    onDragOver: (e: React.DragEvent) => { e.preventDefault(); setIsDragOver(true); },
    onDragLeave: () => setIsDragOver(false),
    onDrop: handleFileDrop,
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
          {...fileDragHandlers}
          className={cn(
            "flex flex-col items-center justify-center gap-3 rounded-xl border-2 border-dashed p-10 cursor-pointer transition-all duration-200",
            isDragOver
              ? "border-primary bg-primary/5 scale-[0.99]"
              : "border-muted-foreground/20 hover:border-primary/40 hover:bg-gray-50/80"
          )}
        >
          <div
            className={cn(
              "h-11 w-11 rounded-xl flex items-center justify-center transition-all duration-200",
              isDragOver ? "bg-primary/10 scale-110" : "bg-muted"
            )}
          >
            <ImagePlus
              className={cn(
                "h-5 w-5 transition-colors",
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

      {/* Sortable grid */}
      {hasImages && (
        <DndContext
          sensors={sensors}
          collisionDetection={closestCenter}
          onDragEnd={handleDragEnd}
        >
          <SortableContext items={images.map((i) => i.id.toString())} strategy={rectSortingStrategy}>
            <div className="grid grid-cols-4 gap-2">
              {images.map((image, index) => (
                <SortableImageCard
                  key={image.id}
                  image={image}
                  index={index}
                  onDelete={onDelete}
                  isDeleting={isDeletingUrl === image.url}
                />
              ))}

              {/* Uploading skeleton tiles */}
              {Array.from({ length: uploadingCount }).map((_, i) => (
                <div
                  key={`uploading-${i}`}
                  className="aspect-square rounded-xl border bg-muted/60 flex items-center justify-center animate-pulse"
                >
                  <Loader2 className="h-5 w-5 animate-spin text-muted-foreground/60" />
                </div>
              ))}

              {/* Add more tile */}
              {images.length + uploadingCount < 20 && (
                <div
                  onClick={() => inputRef.current?.click()}
                  {...fileDragHandlers}
                  className={cn(
                    "aspect-square rounded-xl border-2 border-dashed flex flex-col items-center justify-center cursor-pointer transition-all duration-150 gap-1.5",
                    isDragOver
                      ? "border-primary bg-primary/5 scale-[0.97]"
                      : "border-muted-foreground/20 hover:border-primary/40 hover:bg-gray-50"
                  )}
                >
                  <ImagePlus className="h-4 w-4 text-muted-foreground" />
                  <span className="text-[10px] text-muted-foreground font-medium">Add</span>
                </div>
              )}
            </div>
          </SortableContext>
        </DndContext>
      )}
    </div>
  );
}

// ── ImageCard ──────────────────────────────────────────────────────────

export function ImageCard({ slug, variantId, productBaseId, images: serverImages }: Props) {
  const queryClient = useQueryClient();
  const [localImages, setLocalImages] = useState(serverImages);
  const [deletingUrl, setDeletingUrl] = useState<string | null>(null);

  useEffect(() => {
    setLocalImages(serverImages);
  }, [serverImages]);

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["admin-product", productBaseId] });
    queryClient.invalidateQueries({ queryKey: ["listing", slug] });
    queryClient.invalidateQueries({ queryKey: ["listings"] });
  };

  const reorderMutation = useMutation({
    mutationFn: (ids: number[]) => reorderVariantImages(variantId, ids),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin-product", productBaseId] });
      toast.success("Image order saved");
    },
    onError: (err: unknown) => {
      setLocalImages(serverImages); // rollback optimistic update
      toast.error(getErrorMessage(err, "Failed to save image order"));
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (url: string) => removeListingImage(slug, url),
    onMutate: (url) => setDeletingUrl(url),
    onSuccess: (_, url) => {
      setLocalImages((prev) => prev.filter((img) => img.url !== url));
      setDeletingUrl(null);
      invalidate();
      toast.success("Image removed");
    },
    onError: (err: unknown, _url) => {
      setDeletingUrl(null);
      toast.error(getErrorMessage(err, "Failed to remove image"));
    },
  });

  function handleUploaded(_url: string) {
    // After upload, refetch the product card to get the new image with its server-assigned ID
    invalidate();
  }

  function handleDelete(url: string) {
    deleteMutation.mutate(url);
  }

  function handleReorder(newImages: ProductCardImage[]) {
    setLocalImages(newImages);                              // optimistic
    reorderMutation.mutate(newImages.map((i) => i.id));
  }

  return (
    <div className="bg-card rounded-xl border shadow-sm overflow-hidden">
      <div className="px-6 py-4 border-b bg-gray-50/60 flex items-center justify-between">
        <div>
          <h2 className="text-sm font-semibold text-foreground">Photos</h2>
          <p className="text-xs text-muted-foreground mt-0.5">
            First photo will be the main image. Drag to reorder. PNG, JPG, WebP.
          </p>
        </div>
        {localImages.length > 0 && (
          <span className="text-xs text-muted-foreground">
            {localImages.length} / 20
          </span>
        )}
      </div>
      <div className="p-5">
        <MediaZone
          images={localImages}
          onUploaded={handleUploaded}
          onDelete={handleDelete}
          onReorder={handleReorder}
          slug={slug}
          isDeletingUrl={deletingUrl}
        />
      </div>
    </div>
  );
}
