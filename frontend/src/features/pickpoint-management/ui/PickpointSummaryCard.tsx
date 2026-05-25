"use client";

import { cn } from "@/shared/lib/utils";
import type { PickpointSummary } from "@/entities/pickpoint";

interface Props {
  summary: PickpointSummary;
}

interface StatChip {
  label: string;
  value: number;
  highlight?: boolean;
}

function Chip({ label, value, highlight }: StatChip) {
  return (
    <div className="flex flex-col items-center gap-0.5">
      <span
        className={cn(
          "text-xl font-bold tabular-nums",
          highlight ? "text-rose-600" : "text-foreground",
        )}
      >
        {value}
      </span>
      <span className="text-xs text-muted-foreground">{label}</span>
    </div>
  );
}

export function PickpointSummaryCard({ summary }: Props) {
  return (
    <div className="rounded-xl border bg-card p-5 shadow-sm space-y-4">
      <p className="text-lg font-semibold">{summary.name}</p>
      <div className="grid grid-cols-4 gap-2 text-center">
        <Chip label="Incoming"   value={summary.incoming} />
        <Chip label="Awaiting"   value={summary.awaitingPickup} />
        <Chip label="Overdue"    value={summary.overdue}  highlight={summary.overdue > 0} />
        <Chip label="Returns"    value={summary.customerReturns} />
      </div>
    </div>
  );
}
