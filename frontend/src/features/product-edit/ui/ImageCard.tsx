"use client";

import { useRef, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Upload, Loader2, X, Package } from "lucide-react";
import Image from "next/image";
import { useTranslations } from "next-intl";
import { Button } from "@/shared/ui/button";
import { uploadListingImage, removeListingImage } from "@/entities/product/api";
import { getErrorMessage } from "@/shared/lib";

interface Props {
  slug: string;
  images: string[];
}

export function ImageCard({ slug, images: initialImages }: Props) {
  const t = useTranslations("manage");
  const queryClient = useQueryClient();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [images, setImages] = useState(initialImages);
  const [uploading, setUploading] = useState(false);

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["listing", slug] });
    queryClient.invalidateQueries({ queryKey: ["listings"] });
  };

  const deleteMutation = useMutation({
    mutationFn: (url: string) => removeListingImage(slug, url),
    onSuccess: (_, url) => {
      setImages((prev) => prev.filter((u) => u !== url));
      invalidate();
      toast.success(t("imageRemoved"));
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err, t("failedToRemoveImage"))),
  });

  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    e.target.value = "";

    setUploading(true);
    try {
      await uploadListingImage(slug, file);
      invalidate();
      toast.success(t("imageUploaded"));
    } catch (err: unknown) {
      toast.error(getErrorMessage(err, t("uploadFailed")));
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="bg-card rounded-xl border shadow-sm p-6 space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
          {t("productImages")}
        </h2>
        <Button
          type="button"
          variant="outline"
          size="sm"
          className="gap-1.5"
          disabled={uploading || images.length >= 20}
          onClick={() => fileInputRef.current?.click()}
        >
          {uploading ? (
            <Loader2 className="h-3.5 w-3.5 animate-spin" />
          ) : (
            <Upload className="h-3.5 w-3.5" />
          )}
          {uploading ? t("uploading") : t("addImage")}
        </Button>
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          className="hidden"
          onChange={handleUpload}
        />
      </div>

      <div className="grid grid-cols-4 sm:grid-cols-6 gap-3 relative">
        {uploading && (
          <div className="absolute inset-0 bg-background/60 z-10 flex items-center justify-center rounded-md">
            <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
          </div>
        )}
        {images.length > 0 ? (
          images.map((url, idx) => (
            <div
              key={url}
              className="group relative aspect-square rounded-md border overflow-hidden bg-muted"
            >
              <Image
                src={url}
                alt="Product"
                fill
                className="object-cover"
                unoptimized
              />
              {idx === 0 && (
                <span className="absolute bottom-1 left-1 z-10 rounded bg-black/60 px-1.5 py-0.5 text-[10px] font-medium text-white">
                  {t("primary")}
                </span>
              )}
              <button
                type="button"
                onClick={() => deleteMutation.mutate(url)}
                aria-label="Remove image"
                disabled={deleteMutation.isPending}
                className="absolute top-1 right-1 z-10 rounded-full bg-destructive p-1 text-destructive-foreground opacity-0 group-hover:opacity-100 transition-opacity"
              >
                <X className="h-3 w-3" />
              </button>
            </div>
          ))
        ) : (
          <div className="col-span-4 sm:col-span-6 flex flex-col items-center justify-center rounded-md border border-dashed py-8 text-muted-foreground">
            <Package className="h-8 w-8 mb-2" />
            <p className="text-sm">{t("noImagesYet")}</p>
          </div>
        )}
      </div>

      {images.length >= 20 && (
        <p className="text-xs text-muted-foreground">{t("maxImagesReached")}</p>
      )}
    </div>
  );
}
