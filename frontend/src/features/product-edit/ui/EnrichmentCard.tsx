"use client";

import { useTranslations } from "next-intl";
import { useDraft } from "../model/ProductCardDraftContext";

interface Props {
  variantId: number;
}

export function EnrichmentCard({ variantId }: Props) {
  const t = useTranslations("manage");
  const { draft, updateVariantField } = useDraft();

  const variantDraft = draft.variants[variantId];
  const webDescription = variantDraft?.webDescription ?? "";

  return (
    <div className="bg-card rounded-xl border shadow-sm p-6 space-y-4">
      <h2 className="text-sm font-semibold uppercase tracking-wider text-muted-foreground">
        {t("enrichment")}
      </h2>

      <div className="space-y-1.5">
        <label className="text-sm font-medium">{t("webDescription")}</label>
        <textarea
          value={webDescription}
          onChange={(e) =>
            updateVariantField(variantId, "webDescription", e.target.value)
          }
          rows={4}
          maxLength={5000}
          className="flex w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring resize-none"
        />
        <p className="text-xs text-muted-foreground text-right">
          {webDescription.length} / 5000
        </p>
      </div>
    </div>
  );
}
