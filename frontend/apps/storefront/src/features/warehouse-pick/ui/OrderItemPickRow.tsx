import { useTranslations } from "next-intl";
import { cn } from "@radolfa/shared/lib";
import type { PickSessionItem } from "../types";

interface Props {
  item: PickSessionItem;
  flash: boolean;
}

export function OrderItemPickRow({ item, flash }: Props) {
  const t = useTranslations("warehouse");

  const isPicked = item.quantityPicked === item.quantity;
  const isInProgress = item.quantityPicked > 0 && !isPicked;

  const pill = isPicked
    ? { dot: "bg-green-500", text: "text-green-700 bg-green-50", label: t("pick.session.itemStatus.picked") }
    : isInProgress
      ? { dot: "bg-amber-500", text: "text-amber-700 bg-amber-50", label: t("pick.session.itemStatus.inProgress") }
      : { dot: "bg-zinc-400", text: "text-zinc-600 bg-zinc-100", label: t("pick.session.itemStatus.pending") };

  return (
    <div
      className={cn(
        "flex items-center justify-between rounded-xl border bg-card px-5 py-4 transition-all duration-150",
        flash && "ring-2 ring-green-500",
      )}
    >
      <div className="flex flex-col gap-0.5 min-w-0">
        <span className="font-medium text-sm text-zinc-900 truncate">{item.productName}</span>
        <span className="text-xs text-muted-foreground">{item.sizeLabel}</span>
        <span className="font-mono text-xs text-muted-foreground mt-0.5">{item.barcode}</span>
        {(() => {
          const binPlacements = item.placements.filter((p) => p.binLabel != null);
          const onlyInbound = item.placements.length > 0 && binPlacements.length === 0;
          if (onlyInbound) {
            return (
              <span className="text-xs italic text-muted-foreground mt-0.5">
                {t("pick.session.unassignedHint")}
              </span>
            );
          }
          if (binPlacements.length === 0) return null;
          const MAX_HINTS = 3;
          const shown = binPlacements.slice(0, MAX_HINTS);
          const extra = binPlacements.length - shown.length;
          return (
            <span className="text-xs text-muted-foreground mt-0.5">
              <span className="mr-1">{t("pick.session.binHint")}:</span>
              {shown.map((p, i) => (
                <span key={i}>
                  {i > 0 && <span className="mx-1">·</span>}
                  <span className="font-mono">{p.binLabel}</span>
                  <span className="tabular-nums"> ({p.quantity})</span>
                </span>
              ))}
              {extra > 0 && <span className="ml-1">+{extra}</span>}
            </span>
          );
        })()}
      </div>
      <div className="flex flex-col items-end gap-2 ml-4 shrink-0">
        <span className="text-2xl font-bold tabular-nums">
          {item.quantityPicked} / {item.quantity}
        </span>
        <span className={cn("inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium", pill.text)}>
          <span className={cn("h-1.5 w-1.5 rounded-full", pill.dot)} />
          {pill.label}
        </span>
      </div>
    </div>
  );
}
