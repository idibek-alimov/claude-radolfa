"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { Save, Loader2 } from "lucide-react";
import { useTranslations } from "next-intl";
import { Button } from "@/shared/ui/button";
import { updateListing } from "@/entities/product/api";
import { getErrorMessage } from "@/shared/lib";
import type { ListingVariantDetail } from "@/entities/product/model/types";

interface Props {
  detail: ListingVariantDetail;
}

export function EnrichmentCard({ detail }: Props) {
  const t = useTranslations("manage");
  const queryClient = useQueryClient();

  const [webDescription, setWebDescription] = useState(detail.webDescription ?? "");

  const mutation = useMutation({
    mutationFn: () =>
      updateListing(detail.slug, { webDescription }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["listing", detail.slug] });
      queryClient.invalidateQueries({ queryKey: ["listings"] });
      toast.success(t("productUpdated"));
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err)),
  });

  const isDirty = webDescription !== (detail.webDescription ?? "");

  return (
    <div className="bg-card rounded-xl border shadow-sm p-6 space-y-4">
      <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
        {t("enrichment")}
      </h2>

      <div className="space-y-1.5">
        <label className="text-sm font-medium">{t("webDescription")}</label>
        <textarea
          value={webDescription}
          onChange={(e) => setWebDescription(e.target.value)}
          rows={4}
          maxLength={5000}
          className="flex w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring resize-none"
        />
        <p className="text-xs text-muted-foreground text-right">
          {webDescription.length} / 5000
        </p>
      </div>

      <div className="flex justify-end">
        <Button
          onClick={() => mutation.mutate()}
          disabled={mutation.isPending || !isDirty}
        >
          {mutation.isPending ? (
            <>
              <Loader2 className="h-4 w-4 mr-1.5 animate-spin" />
              {t("saving")}
            </>
          ) : (
            <>
              <Save className="h-4 w-4 mr-1.5" />
              {t("save")}
            </>
          )}
        </Button>
      </div>
    </div>
  );
}
