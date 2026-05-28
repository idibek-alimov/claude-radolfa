"use client";

import { Info, Loader2, Save, SendHorizonal, Undo2 } from "lucide-react";
import { Button } from "@/shared/ui/button";
import { useTranslations } from "next-intl";
import { useDraft } from "../model/ProductCardDraftContext";
import { ProductStatus } from "@/entities/product/model/types";
import { useSubmitProductForReview } from "@/entities/product/api/moderation";

interface Props {
  onSave: () => void;
  onDiscard: () => void;
  isSaving: boolean;
  status: ProductStatus;
  productBaseId: number;
}

export function SaveBar({ onSave, onDiscard, isSaving, status, productBaseId }: Props) {
  const { isDirty, diff } = useDraft();
  const t = useTranslations("manage.products.saveBar");
  const submitMutation = useSubmitProductForReview();

  if (isDirty) {
    return (
      <div className="sticky bottom-0 z-30 bg-card border-t shadow-lg px-8 py-3 flex items-center justify-between">
        <div className="flex flex-col gap-0.5">
          <span className="text-sm text-muted-foreground">
            {diff.length} unsaved change{diff.length !== 1 ? "s" : ""}
          </span>
          {status === ProductStatus.PENDING_REVIEW && (
            <span className="text-xs text-amber-600">{t("dirtyResetsToDraft")}</span>
          )}
        </div>
        <div className="flex gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={onDiscard}
            disabled={isSaving}
          >
            <Undo2 className="h-3.5 w-3.5 mr-1.5" />
            Discard
          </Button>
          <Button size="sm" onClick={onSave} disabled={isSaving}>
            {isSaving ? (
              <Loader2 className="h-3.5 w-3.5 mr-1.5 animate-spin" />
            ) : (
              <Save className="h-3.5 w-3.5 mr-1.5" />
            )}
            Save Changes
          </Button>
        </div>
      </div>
    );
  }

  if (status === ProductStatus.DRAFT) {
    return (
      <div className="sticky bottom-0 z-30 bg-card border-t shadow-lg px-8 py-3 flex items-center justify-end">
        <Button
          size="sm"
          onClick={() => submitMutation.mutate(productBaseId)}
          disabled={submitMutation.isPending}
        >
          {submitMutation.isPending ? (
            <Loader2 className="h-3.5 w-3.5 mr-1.5 animate-spin" />
          ) : (
            <SendHorizonal className="h-3.5 w-3.5 mr-1.5" />
          )}
          {t("submitForReview")}
        </Button>
      </div>
    );
  }

  if (status === ProductStatus.PENDING_REVIEW) {
    return (
      <div className="sticky bottom-0 z-30 bg-card border-t shadow-lg px-8 py-3 flex items-center gap-2 text-amber-700">
        <Info className="h-4 w-4 shrink-0" />
        <span className="text-sm">{t("underReviewNotice")}</span>
      </div>
    );
  }

  return null;
}
