"use client";

import { useState, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/shared/ui/button";
import { Skeleton } from "@/shared/ui/skeleton";
import { fetchDiscounts } from "../api";
import { cn } from "@/shared/lib";

interface Props {
  selectedDate: string | null; // "YYYY-MM-DD" or null
  onDayClick: (dateIso: string | null) => void;
}

const WEEKDAYS = ["Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"];
const DAY_MS = 86_400_000;

function startOfDay(d: Date): Date {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate());
}

function startOfMonth(d: Date): Date {
  return new Date(d.getFullYear(), d.getMonth(), 1);
}

function toYMD(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

function daysInMonth(d: Date): number {
  return new Date(d.getFullYear(), d.getMonth() + 1, 0).getDate();
}

function firstWeekdayOffset(d: Date): number {
  // Monday-based: Mon=0 … Sun=6
  const dow = d.getDay(); // 0=Sun
  return (dow + 6) % 7;
}

export function DiscountCalendar({ selectedDate, onDayClick }: Props) {
  const today = startOfDay(new Date());
  const [month, setMonth] = useState<Date>(() => startOfMonth(new Date()));

  const monthStartIso = month.toISOString();
  const monthEnd = new Date(month.getFullYear(), month.getMonth() + 1, 0, 23, 59, 59, 999);
  const monthEndIso = monthEnd.toISOString();

  const { data, isLoading } = useQuery({
    queryKey: ["discounts-calendar", monthStartIso],
    queryFn: () =>
      fetchDiscounts({ from: monthStartIso, to: monthEndIso, page: 1, size: 200 }),
    staleTime: 60_000,
  });

  // Compute per-day flags for all days in this month
  const dayFlags = useMemo(() => {
    const campaigns = data?.content ?? [];
    const total = daysInMonth(month);
    const flags: { running: boolean; starting: boolean; ending: boolean }[] = Array.from(
      { length: total },
      () => ({ running: false, starting: false, ending: false })
    );

    for (const c of campaigns) {
      const from = startOfDay(new Date(c.validFrom));
      const upto = startOfDay(new Date(c.validUpto));

      for (let i = 0; i < total; i++) {
        const dayDate = new Date(month.getFullYear(), month.getMonth(), i + 1);
        const dayTs = dayDate.getTime();
        const fromTs = from.getTime();
        const uptoTs = upto.getTime();

        if (dayTs === fromTs) flags[i].starting = true;
        if (dayTs === uptoTs) flags[i].ending = true;
        if (dayTs > fromTs && dayTs < uptoTs) flags[i].running = true;
        // Also mark starting day as running
        if (dayTs === fromTs && fromTs <= uptoTs) flags[i].running = true;
      }
    }
    return flags;
  }, [data, month]);

  const totalDays = daysInMonth(month);
  const offset = firstWeekdayOffset(month); // blank cells before day 1

  const monthLabel = month.toLocaleDateString("en-US", { month: "long", year: "numeric" });

  function prevMonth() {
    setMonth((m) => new Date(m.getFullYear(), m.getMonth() - 1, 1));
  }
  function nextMonth() {
    setMonth((m) => new Date(m.getFullYear(), m.getMonth() + 1, 1));
  }
  function goToday() {
    setMonth(startOfMonth(new Date()));
    onDayClick(null);
  }

  function handleDayClick(dayIndex: number) {
    const d = new Date(month.getFullYear(), month.getMonth(), dayIndex + 1);
    const iso = toYMD(d);
    onDayClick(selectedDate === iso ? null : iso);
  }

  const todayYMD = toYMD(today);

  return (
    <div className="rounded-xl border bg-card p-4 shrink-0 select-none">
      {/* Header */}
      <div className="flex items-center gap-2 mb-3">
        <Button variant="ghost" size="sm" className="h-7 w-7 p-0" onClick={prevMonth}>
          <ChevronLeft className="h-3.5 w-3.5" />
        </Button>
        <span className="flex-1 text-center text-sm font-medium">{monthLabel}</span>
        <Button variant="ghost" size="sm" className="h-7 w-7 p-0" onClick={nextMonth}>
          <ChevronRight className="h-3.5 w-3.5" />
        </Button>
        <Button variant="ghost" size="sm" className="h-7 px-2 text-xs" onClick={goToday}>
          Today
        </Button>
      </div>

      {/* Weekday labels */}
      <div className="grid grid-cols-7 mb-1">
        {WEEKDAYS.map((d) => (
          <div key={d} className="text-center text-xs text-muted-foreground font-medium py-0.5">
            {d}
          </div>
        ))}
      </div>

      {/* Day grid */}
      <div className="grid grid-cols-7 gap-0.5 relative">
        {isLoading && (
          <div className="absolute inset-0 z-10 rounded-lg overflow-hidden">
            <Skeleton className="w-full h-full opacity-40" />
          </div>
        )}

        {/* Blank offset cells */}
        {Array.from({ length: offset }, (_, i) => (
          <div key={`blank-${i}`} />
        ))}

        {/* Day cells */}
        {Array.from({ length: totalDays }, (_, i) => {
          const dayDate = new Date(month.getFullYear(), month.getMonth(), i + 1);
          const iso = toYMD(dayDate);
          const isToday = iso === todayYMD;
          const isSelected = iso === selectedDate;
          const { running, starting, ending } = dayFlags[i];
          const hasDots = running || starting || ending;

          return (
            <button
              key={iso}
              onClick={() => handleDayClick(i)}
              className={cn(
                "relative flex flex-col items-center justify-between rounded-lg px-0.5 py-1 h-11 text-xs transition-colors hover:bg-muted/60",
                isSelected && "bg-primary/10 ring-1 ring-primary/50",
                isToday && !isSelected && "ring-1 ring-primary/30"
              )}
            >
              <span
                className={cn(
                  "font-medium leading-none mt-0.5",
                  isSelected ? "text-primary" : isToday ? "text-primary" : "text-foreground"
                )}
              >
                {i + 1}
              </span>
              {/* Dots */}
              {hasDots && (
                <div className="flex items-center gap-0.5 mb-0.5">
                  {running && (
                    <span className="h-1.5 w-1.5 rounded-full bg-green-500 shrink-0" />
                  )}
                  {starting && (
                    <span className="h-1.5 w-1.5 rounded-full bg-blue-500 shrink-0" />
                  )}
                  {ending && (
                    <span className="h-1.5 w-1.5 rounded-full bg-orange-400 shrink-0" />
                  )}
                </div>
              )}
              {!hasDots && <span className="h-1.5" />}
            </button>
          );
        })}
      </div>

      {/* Legend */}
      <div className="flex items-center gap-3 mt-3 pt-2.5 border-t">
        <span className="flex items-center gap-1 text-xs text-muted-foreground">
          <span className="h-2 w-2 rounded-full bg-green-500 shrink-0" /> Running
        </span>
        <span className="flex items-center gap-1 text-xs text-muted-foreground">
          <span className="h-2 w-2 rounded-full bg-blue-500 shrink-0" /> Starting
        </span>
        <span className="flex items-center gap-1 text-xs text-muted-foreground">
          <span className="h-2 w-2 rounded-full bg-orange-400 shrink-0" /> Ending
        </span>
      </div>
    </div>
  );
}
