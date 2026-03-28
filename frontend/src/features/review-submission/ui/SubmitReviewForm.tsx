"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/shared/ui/dialog";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Textarea } from "@/shared/ui/textarea";
import { Label } from "@/shared/ui/label";
import { fetchMyDeliveredOrders } from "@/entities/order";
import { submitReview } from "@/entities/review";
import type { MatchingSize } from "@/entities/review";
import { getErrorMessage } from "@/shared/lib";

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
    !mutation.isPending;

  function handleSubmit() {
    if (!canSubmit || selectedOrderId === null) return;
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
      photoUrls: [],
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

            <Button
              className="w-full"
              disabled={!canSubmit}
              onClick={handleSubmit}
            >
              {mutation.isPending ? "Submitting…" : "Submit Review"}
            </Button>
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
