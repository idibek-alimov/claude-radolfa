"use client";

import { useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { useTranslations } from "next-intl";
import { X } from "lucide-react";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/shared/ui/dialog";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Textarea } from "@/shared/ui/textarea";
import { Label } from "@/shared/ui/label";
import { fetchMyDeliveredOrders } from "@/entities/order";
import { submitReview, uploadReviewPhotos } from "@/entities/review";
import type { MatchingSize } from "@/entities/review";
import { getErrorMessage } from "@/shared/lib";

const MAX_PHOTOS = 5;

interface SubmitReviewFormProps {
  listingVariantId: number;
  slug: string;
}

const sizeFitOptions: { value: MatchingSize; label: string }[] = [
  { value: "RUNS_SMALL", label: "Runs small" },
  { value: "ACCURATE",   label: "True to size" },
  { value: "RUNS_LARGE", label: "Runs large" },
];

export function SubmitReviewForm({ listingVariantId, slug }: SubmitReviewFormProps) {
  const tForm = useTranslations("reviews.form");

  const [open, setOpen] = useState(false);
  const [selectedOrderId, setSelectedOrderId] = useState<number | null>(null);
  const [selectedSkuId, setSelectedSkuId] = useState<number | null>(null);
  const [rating, setRating] = useState(0);
  const [hoverRating, setHoverRating] = useState(0);
  const [title, setTitle] = useState("");
  const [body, setBody] = useState("");
  const [pros, setPros] = useState("");
  const [cons, setCons] = useState("");
  const [matchingSize, setMatchingSize] = useState<MatchingSize | null>(null);
  const [photoFiles, setPhotoFiles] = useState<File[]>([]);
  const [isUploading, setIsUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const qc = useQueryClient();

  const { data: deliveredOrders = [], isLoading: ordersLoading } = useQuery({
    queryKey: ["my-delivered-orders"],
    queryFn: fetchMyDeliveredOrders,
    enabled: open,
  });

  const eligibleOrders = deliveredOrders.filter((o) =>
    o.items.some((item) => item.listingVariantId === listingVariantId)
  );

  const mutation = useMutation({
    mutationFn: submitReview,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["rating", slug] });
      qc.invalidateQueries({ queryKey: ["reviews", slug] });
      handleClose();
      toast.success("Review submitted — it will appear after approval.");
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  function handleClose() {
    setOpen(false);
    setSelectedOrderId(null);
    setSelectedSkuId(null);
    setRating(0);
    setHoverRating(0);
    setTitle("");
    setBody("");
    setPros("");
    setCons("");
    setMatchingSize(null);
    photoFiles.forEach((f) => URL.revokeObjectURL(URL.createObjectURL(f)));
    setPhotoFiles([]);
    setIsUploading(false);
    if (fileInputRef.current) fileInputRef.current.value = "";
  }

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const selected = Array.from(e.target.files ?? []);
    setPhotoFiles((prev) => {
      const slots = MAX_PHOTOS - prev.length;
      return [...prev, ...selected.slice(0, slots)];
    });
    if (fileInputRef.current) fileInputRef.current.value = "";
  }

  function removePhoto(index: number) {
    setPhotoFiles((prev) => prev.filter((_, i) => i !== index));
  }

  function handleOrderChange(id: number) {
    setSelectedOrderId(id);
    const order = eligibleOrders.find((o) => o.id === id);
    const item = order?.items.find((i) => i.listingVariantId === listingVariantId);
    setSelectedSkuId(item?.skuId ?? null);
  }

  const canSubmit =
    selectedOrderId !== null &&
    rating > 0 &&
    body.trim().length > 0 &&
    !mutation.isPending &&
    !isUploading;

  async function handleSubmit() {
    if (!canSubmit || selectedOrderId === null) return;

    let photoUrls: string[] = [];
    if (photoFiles.length > 0) {
      setIsUploading(true);
      try {
        const result = await uploadReviewPhotos(photoFiles);
        photoUrls = result.urls;
      } catch (err) {
        toast.error(getErrorMessage(err));
        setIsUploading(false);
        return;
      }
      setIsUploading(false);
    }

    mutation.mutate({
      listingVariantId,
      skuId: selectedSkuId,
      orderId: selectedOrderId,
      rating,
      title: title.trim() || null,
      body: body.trim(),
      pros: pros.trim() || null,
      cons: cons.trim() || null,
      matchingSize,
      photoUrls,
    });
  }

  const noEligibleOrders = !ordersLoading && eligibleOrders.length === 0;

  return (
    <Dialog open={open} onOpenChange={(v) => { if (!v) handleClose(); else setOpen(true); }}>
      <DialogTrigger asChild>
        <Button variant="outline" size="sm">Write a Review</Button>
      </DialogTrigger>

      <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>Write a Review</DialogTitle>
        </DialogHeader>

        {ordersLoading ? (
          <div className="space-y-3 animate-pulse">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-8 rounded bg-muted" />
            ))}
          </div>
        ) : noEligibleOrders ? (
          <p className="text-sm text-muted-foreground py-2">
            You can only review products from a delivered order containing this item.
          </p>
        ) : (
          <div className="space-y-4 pt-1">
            {/* Order selector */}
            <div className="space-y-1.5">
              <Label htmlFor="review-order">Order</Label>
              <select
                id="review-order"
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                value={selectedOrderId ?? ""}
                onChange={(e) => handleOrderChange(Number(e.target.value))}
              >
                <option value="" disabled>Select order…</option>
                {eligibleOrders.map((o) => (
                  <option key={o.id} value={o.id}>
                    Order #{o.id} — {new Date(o.createdAt).toLocaleDateString()}
                  </option>
                ))}
              </select>
            </div>

            {/* Star rating */}
            <div className="space-y-1.5">
              <Label>Rating *</Label>
              <div
                className="flex gap-1"
                onMouseLeave={() => setHoverRating(0)}
              >
                {[1, 2, 3, 4, 5].map((s) => (
                  <button
                    key={s}
                    type="button"
                    onClick={() => setRating(s)}
                    onMouseEnter={() => setHoverRating(s)}
                    className={`text-2xl transition-colors ${
                      s <= (hoverRating || rating)
                        ? "text-amber-400"
                        : "text-muted-foreground"
                    }`}
                    aria-label={`${s} star${s > 1 ? "s" : ""}`}
                  >
                    ★
                  </button>
                ))}
              </div>
            </div>

            {/* Title */}
            <div className="space-y-1.5">
              <Label htmlFor="review-title">Title</Label>
              <Input
                id="review-title"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                maxLength={255}
                placeholder="Summarise your experience"
              />
            </div>

            {/* Body */}
            <div className="space-y-1.5">
              <Label htmlFor="review-body">Review *</Label>
              <Textarea
                id="review-body"
                value={body}
                onChange={(e) => setBody(e.target.value)}
                maxLength={5000}
                rows={4}
                placeholder="Tell others what you think…"
              />
            </div>

            {/* Pros */}
            <div className="space-y-1.5">
              <Label htmlFor="review-pros">Pros</Label>
              <Input
                id="review-pros"
                value={pros}
                onChange={(e) => setPros(e.target.value)}
                maxLength={1000}
                placeholder="What did you like?"
              />
            </div>

            {/* Cons */}
            <div className="space-y-1.5">
              <Label htmlFor="review-cons">Cons</Label>
              <Input
                id="review-cons"
                value={cons}
                onChange={(e) => setCons(e.target.value)}
                maxLength={1000}
                placeholder="What could be better?"
              />
            </div>

            {/* Size fit */}
            <div className="space-y-1.5">
              <Label>Size fit</Label>
              <div className="flex gap-2 flex-wrap">
                {sizeFitOptions.map(({ value, label }) => (
                  <button
                    key={value}
                    type="button"
                    onClick={() => setMatchingSize(matchingSize === value ? null : value)}
                    className={`text-sm px-3 py-1 rounded-full border transition-colors ${
                      matchingSize === value
                        ? "bg-foreground text-background border-foreground"
                        : "border-border text-muted-foreground hover:border-foreground"
                    }`}
                  >
                    {label}
                  </button>
                ))}
              </div>
            </div>

            {/* Photos */}
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <Label>Photos</Label>
                <span className="text-xs text-muted-foreground">{tForm("photoLimit")}</span>
              </div>

              {photoFiles.length > 0 && (
                <div className="flex flex-wrap gap-2">
                  {photoFiles.map((file, i) => (
                    <div key={i} className="relative h-14 w-14 shrink-0">
                      {/* eslint-disable-next-line @next/next/no-img-element */}
                      <img
                        src={URL.createObjectURL(file)}
                        alt={`Photo ${i + 1}`}
                        className="h-full w-full rounded-lg object-cover border"
                      />
                      <button
                        type="button"
                        onClick={() => removePhoto(i)}
                        className="absolute -right-1.5 -top-1.5 flex h-5 w-5 items-center justify-center rounded-full bg-foreground text-background shadow"
                        aria-label="Remove photo"
                      >
                        <X className="h-3 w-3" />
                      </button>
                    </div>
                  ))}
                </div>
              )}

              {photoFiles.length < MAX_PHOTOS && (
                <>
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/jpeg,image/png,image/webp"
                    multiple
                    className="hidden"
                    onChange={handleFileChange}
                  />
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => fileInputRef.current?.click()}
                  >
                    {tForm("addPhotos")}
                  </Button>
                </>
              )}
            </div>

            <Button
              className="w-full"
              disabled={!canSubmit}
              onClick={handleSubmit}
            >
              {isUploading
                ? tForm("uploading")
                : mutation.isPending
                  ? "Submitting…"
                  : "Submit Review"}
            </Button>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
