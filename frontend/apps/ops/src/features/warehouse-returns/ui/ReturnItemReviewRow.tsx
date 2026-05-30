"use client";

import { useTranslations } from "next-intl";
import { ToggleGroup, ToggleGroupItem } from "@radolfa/shared/ui/toggle-group";
import { cn } from "@radolfa/shared/lib";
import { formatPrice } from "@radolfa/shared/lib/format";
import type { CustomerReturnItem, Resellability, ReturnReason } from "@/entities/pickpoint";

const REASON_STYLES: Record<ReturnReason, string> = {
  DAMAGED:          "bg-rose-50 text-rose-700",
  WRONG_ITEM:       "bg-amber-50 text-amber-700",
  NOT_AS_DESCRIBED: "bg-orange-50 text-orange-700",
  CHANGED_MIND:     "bg-slate-100 text-slate-600",
  OTHER:            "bg-zinc-100 text-zinc-600",
};

interface Props {
  item: CustomerReturnItem;
  chosen: Resellability | undefined;
  onChange: (value: Resellability) => void;
}

export function ReturnItemReviewRow({ item, chosen, onChange }: Props) {
  const t = useTranslations("warehouse");
  const alreadyReviewed = item.resellability !== "PENDING_REVIEW";
  const effectiveValue = alreadyReviewed ? item.resellability : chosen;

  return (
    <div className="flex items-start gap-6 py-4 border-b last:border-0">
      {/* Item info */}
      <div className="flex-1 min-w-0">
        <p className="font-medium text-sm leading-snug">
          {item.productName ?? "—"}
          {item.sizeLabel && (
            <span className="text-muted-foreground font-normal"> — {item.sizeLabel}</span>
          )}
        </p>
        <p className="text-xs text-muted-foreground tabular-nums mt-0.5">
          {item.skuCode} · ×{item.quantity}
        </p>
        <p className="text-xs text-muted-foreground mt-0.5">
          {t("returns.review.refundLabel")}: {formatPrice(item.refundAmount)}
        </p>
        <div className="flex items-center gap-2 mt-1.5">
          <span
            className={cn(
              "inline-block rounded-full px-2 py-0.5 text-xs font-medium",
              REASON_STYLES[item.reason] ?? "bg-zinc-100 text-zinc-600"
            )}
          >
            {t(`returns.reason.${item.reason}`)}
          </span>
          {item.notes && (
            <span className="text-xs italic text-muted-foreground truncate">{item.notes}</span>
          )}
        </div>
      </div>

      {/* Review action */}
      <div className="shrink-0 flex flex-col items-end gap-1">
        {alreadyReviewed ? (
          <>
            <span
              className={cn(
                "inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1",
                item.resellability === "RESELLABLE"
                  ? "bg-green-50 text-green-700 ring-green-200"
                  : "bg-rose-50 text-rose-700 ring-rose-200"
              )}
            >
              {item.resellability === "RESELLABLE"
                ? t("returns.review.resellable")
                : t("returns.review.defective")}
            </span>
            <span className="text-xs text-muted-foreground">{t("returns.review.alreadyReviewed")}</span>
          </>
        ) : (
          <ToggleGroup
            type="single"
            value={effectiveValue ?? ""}
            onValueChange={(v) => {
              if (v === "RESELLABLE" || v === "DEFECTIVE") onChange(v);
            }}
          >
            <ToggleGroupItem
              value="RESELLABLE"
              className="data-[state=on]:bg-green-600 data-[state=on]:text-white data-[state=on]:border-green-600"
            >
              {t("returns.review.resellable")}
            </ToggleGroupItem>
            <ToggleGroupItem
              value="DEFECTIVE"
              className="data-[state=on]:bg-rose-600 data-[state=on]:text-white data-[state=on]:border-rose-600"
            >
              {t("returns.review.defective")}
            </ToggleGroupItem>
          </ToggleGroup>
        )}
      </div>
    </div>
  );
}
