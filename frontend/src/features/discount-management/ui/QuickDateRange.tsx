"use client";

import { Button } from "@/shared/ui/button";

interface QuickDateRangeProps {
  onSelect: (from: string, to: string) => void;
}

function toLocalInput(d: Date) {
  // Returns "YYYY-MM-DDTHH:mm"
  const pad = (n: number) => String(n).padStart(2, "0");
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` +
    `T${pad(d.getHours())}:${pad(d.getMinutes())}`
  );
}

const PRESETS = [
  {
    label: "7 days",
    days: 7,
  },
  {
    label: "14 days",
    days: 14,
  },
  {
    label: "30 days",
    days: 30,
  },
  {
    label: "This month",
    days: null as null | number,
  },
];

export function QuickDateRange({ onSelect }: QuickDateRangeProps) {
  const apply = (days: number | null) => {
    const now = new Date();
    now.setSeconds(0, 0);
    let end: Date;
    if (days === null) {
      // last day of current month
      end = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59);
    } else {
      end = new Date(now.getTime() + days * 24 * 60 * 60 * 1000);
    }
    onSelect(toLocalInput(now), toLocalInput(end));
  };

  return (
    <div className="flex flex-wrap gap-1.5">
      {PRESETS.map((p) => (
        <Button
          key={p.label}
          type="button"
          variant="outline"
          size="sm"
          className="h-7 text-xs px-2.5"
          onClick={() => apply(p.days)}
        >
          {p.label}
        </Button>
      ))}
    </div>
  );
}
