"use client";

import { useState, useMemo } from "react";
import Link from "next/link";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Skeleton } from "@/shared/ui/skeleton";
import { Button } from "@/shared/ui/button";
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
import { Tag, Crown, CalendarOff, ExternalLink, X } from "lucide-react";
import { toast } from "sonner";
import {
  fetchProductCampaigns,
  fetchDiscountById,
  updateDiscount,
} from "@/features/discount-management/api";
import { getErrorMessage } from "@/shared/lib";
import type { CampaignSummary } from "@/features/discount-management/model/types";

interface Props {
  productBaseId: number;
  allSkuCodes: string[];
}

export function ProductCampaignsPanel({ productBaseId, allSkuCodes }: Props) {
  const qc = useQueryClient();
  const [removingId, setRemovingId] = useState<number | null>(null);

  const { data: campaigns = [], isLoading, isError, error } = useQuery({
    queryKey: ["product-campaigns", productBaseId],
    queryFn: () => fetchProductCampaigns(productBaseId),
    staleTime: 30_000,
  });

  // Winner = first campaign (backend sorts by rank asc, then id asc)
  const winnerId = useMemo(() => campaigns[0]?.id ?? null, [campaigns]);

  const removeMutation = useMutation({
    mutationFn: async (campaignId: number) => {
      const full = await fetchDiscountById(campaignId);
      const remainingSkuCodes = full.targets
        .filter((t) => t.targetType === "SKU")
        .map((t) => t.referenceId)
        .filter((code) => !allSkuCodes.includes(code));
      const newTargets = [
        ...remainingSkuCodes.map((code) => ({ targetType: "SKU" as const, referenceId: code })),
        ...full.targets.filter((t) => t.targetType !== "SKU"),
      ];
      if (newTargets.length === 0) {
        throw new Error(
          "Cannot remove — campaign would have zero targets. Delete the campaign instead."
        );
      }
      return updateDiscount(campaignId, {
        typeId: full.type.id,
        targets: newTargets,
        amountType: full.amountType,
        amountValue: full.amountValue,
        validFrom: full.validFrom,
        validUpto: full.validUpto,
        title: full.title,
        colorHex: full.colorHex,
      });
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["product-campaigns", productBaseId] });
      qc.invalidateQueries({ queryKey: ["discounts"] });
      qc.invalidateQueries({ queryKey: ["discount-overlaps"] });
      toast.success("Product removed from campaign");
      setRemovingId(null);
    },
    onError: (err) => {
      toast.error(getErrorMessage(err));
      setRemovingId(null);
    },
  });

  const removingCampaign = campaigns.find((c) => c.id === removingId);

  return (
    <section className="bg-card border rounded-xl p-5 space-y-4">
      {/* Heading */}
      <div className="flex items-center gap-2">
        <Tag className="h-4 w-4 text-muted-foreground" />
        <h3 className="text-sm font-semibold text-foreground">Discount Campaigns</h3>
        {campaigns.length > 0 && (
          <span className="ml-auto text-xs font-medium bg-muted rounded-full px-2 py-0.5 text-muted-foreground">
            {campaigns.length}
          </span>
        )}
      </div>

      {/* Loading */}
      {isLoading && (
        <div className="space-y-2">
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-14 rounded-lg" />
          ))}
        </div>
      )}

      {/* Error */}
      {isError && (
        <p className="text-sm text-muted-foreground">{getErrorMessage(error)}</p>
      )}

      {/* Empty state */}
      {!isLoading && !isError && campaigns.length === 0 && (
        <div className="border border-dashed rounded-xl p-8 flex flex-col items-center gap-3 text-center">
          <CalendarOff className="h-10 w-10 text-muted-foreground/40" />
          <p className="text-sm text-muted-foreground">
            No active campaigns touch this product.
          </p>
          <Button variant="outline" size="sm" asChild>
            <Link href="/manage/discounts/create">Create campaign</Link>
          </Button>
        </div>
      )}

      {/* Campaign rows */}
      {!isLoading && !isError && campaigns.length > 0 && (
        <div className="space-y-2">
          {campaigns.map((campaign: CampaignSummary) => (
            <CampaignRow
              key={campaign.id}
              campaign={campaign}
              isWinner={campaign.id === winnerId}
              isRemoving={removeMutation.isPending && removingId === campaign.id}
              onRemove={() => setRemovingId(campaign.id)}
            />
          ))}
        </div>
      )}

      {/* Remove confirmation dialog */}
      <AlertDialog
        open={removingId !== null}
        onOpenChange={(open) => { if (!open) setRemovingId(null); }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Remove from campaign?</AlertDialogTitle>
            <AlertDialogDescription>
              This will remove{" "}
              <span className="font-medium">{allSkuCodes.length} SKU{allSkuCodes.length !== 1 ? "s" : ""}</span>{" "}
              of this product from{" "}
              <span className="font-medium">&ldquo;{removingCampaign?.title}&rdquo;</span>.
              The campaign will continue for other products.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancel</AlertDialogCancel>
            <AlertDialogAction
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
              onClick={() => {
                if (removingId !== null) removeMutation.mutate(removingId);
              }}
            >
              Remove
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </section>
  );
}

function CampaignRow({
  campaign,
  isWinner,
  isRemoving,
  onRemove,
}: {
  campaign: CampaignSummary;
  isWinner: boolean;
  isRemoving: boolean;
  onRemove: () => void;
}) {
  return (
    <div className="flex items-center gap-3 rounded-lg border bg-muted/20 overflow-hidden">
      {/* Colored rail */}
      <div
        className="w-1 self-stretch shrink-0 rounded-l-lg"
        style={{ backgroundColor: `#${campaign.colorHex}` }}
      />

      {/* Info */}
      <div className="flex-1 min-w-0 py-3 pr-1">
        <div className="flex items-center gap-1.5 flex-wrap">
          {isWinner && (
            <Crown className="h-3.5 w-3.5 text-amber-500 shrink-0" />
          )}
          <span className="text-sm font-medium text-foreground truncate">
            {campaign.title}
          </span>
          <span className="text-sm font-bold text-rose-600 tabular-nums shrink-0">
            −{campaign.amountValue}{campaign.amountType === "FIXED" ? " TJS" : "%"}
          </span>
        </div>
        <div className="flex items-center gap-1.5 mt-0.5">
          <span className="text-xs text-muted-foreground">{campaign.type.name}</span>
          <span className="text-xs text-muted-foreground/50">·</span>
          <span className="text-xs text-muted-foreground">rank {campaign.type.rank}</span>
        </div>
      </div>

      {/* Actions */}
      <div className="flex items-center gap-1 pr-2 shrink-0">
        <Button variant="ghost" size="sm" className="h-7 px-2 gap-1 text-xs" asChild>
          <Link href={`/manage/discounts/${campaign.id}/edit`}>
            <ExternalLink className="h-3 w-3" />
            Open
          </Link>
        </Button>
        <Button
          variant="ghost"
          size="sm"
          className="h-7 px-2 gap-1 text-xs text-destructive hover:text-destructive hover:bg-destructive/10"
          onClick={onRemove}
          disabled={isRemoving}
        >
          <X className="h-3 w-3" />
          Remove
        </Button>
      </div>
    </div>
  );
}
