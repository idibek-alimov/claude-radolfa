"use client";

import { useMemo, useState, useEffect, useRef } from "react";
import { useQuery } from "@tanstack/react-query";
import { ChevronLeft, ChevronRight, CalendarRange, AlertTriangle } from "lucide-react";
import { Button } from "@/shared/ui/button";
import { Skeleton } from "@/shared/ui/skeleton";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/shared/ui/tooltip";
import { fetchDiscounts, fetchDiscountOverlaps } from "../api";
import type { DiscountResponse } from "../model/types";
import { getErrorMessage } from "@/shared/lib/utils";

// ── Constants ─────────────────────────────────────────────────────────────────

const WINDOW_DAYS = 30;
const ROW_HEIGHT = 40;
const HEADER_HEIGHT = 56;
const LEFT_GUTTER = 240;
const DAY_MS = 86_400_000;

// ── Helpers ───────────────────────────────────────────────────────────────────

type DerivedStatus = "active" | "scheduled" | "expired" | "disabled";

function getStatus(d: DiscountResponse): DerivedStatus {
  if (d.disabled) return "disabled";
  const now = new Date();
  if (new Date(d.validUpto) < now) return "expired";
  if (new Date(d.validFrom) > now) return "scheduled";
  return "active";
}

function startOfDay(d: Date): Date {
  return new Date(d.getFullYear(), d.getMonth(), d.getDate());
}

function shiftDays(d: Date, n: number): Date {
  return new Date(d.getTime() + n * DAY_MS);
}

function fmtDate(d: Date): string {
  return d.toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" });
}

function fmtDateShort(d: Date): string {
  return d.toLocaleDateString("en-US", { month: "short", day: "numeric" });
}

// ── Props ─────────────────────────────────────────────────────────────────────

interface Props {
  onEdit: (d: DiscountResponse) => void;
}

// ── Component ─────────────────────────────────────────────────────────────────

export function DiscountTimeline({ onEdit }: Props) {
  const [windowStart, setWindowStart] = useState(() =>
    shiftDays(startOfDay(new Date()), -Math.floor(WINDOW_DAYS / 2))
  );
  const windowEnd = shiftDays(windowStart, WINDOW_DAYS);

  const containerRef = useRef<HTMLDivElement>(null);
  const [chartWidth, setChartWidth] = useState(800);

  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;
    const obs = new ResizeObserver((entries) => {
      const w = entries[0].contentRect.width - LEFT_GUTTER - 32;
      setChartWidth(Math.max(320, Math.floor(w)));
    });
    obs.observe(el);
    return () => obs.disconnect();
  }, []);

  const windowStartISO = windowStart.toISOString();
  const windowEndISO = windowEnd.toISOString();

  const { data, isLoading, error } = useQuery({
    queryKey: ["discounts-timeline", windowStartISO, windowEndISO],
    queryFn: () =>
      fetchDiscounts({ from: windowStartISO, to: windowEndISO, page: 1, size: 200 }),
    staleTime: 30_000,
  });

  const { data: overlaps } = useQuery({
    queryKey: ["discount-overlaps"],
    queryFn: fetchDiscountOverlaps,
    staleTime: 60_000,
  });

  // ── Swim-lane grouping (connected components) ─────────────────────────────

  const rows = useMemo(() => {
    const campaigns = data?.content ?? [];
    if (campaigns.length === 0) return [];

    const idSet = new Set(campaigns.map((c) => c.id));
    const adj = new Map<number, Set<number>>();
    for (const c of campaigns) adj.set(c.id, new Set());

    if (overlaps) {
      for (const row of overlaps) {
        const ids = [row.winningCampaign.id, ...row.losingCampaigns.map((c) => c.id)].filter(
          (id) => idSet.has(id)
        );
        for (const a of ids) {
          for (const b of ids) {
            if (a !== b) adj.get(a)!.add(b);
          }
        }
      }
    }

    const seen = new Set<number>();
    const groups: DiscountResponse[][] = [];
    for (const c of campaigns) {
      if (seen.has(c.id)) continue;
      const group: DiscountResponse[] = [];
      const queue: number[] = [c.id];
      while (queue.length) {
        const id = queue.shift()!;
        if (seen.has(id)) continue;
        seen.add(id);
        const found = campaigns.find((x) => x.id === id);
        if (found) group.push(found);
        for (const nb of adj.get(id) ?? []) queue.push(nb);
      }
      group.sort((a, b) => new Date(a.validFrom).getTime() - new Date(b.validFrom).getTime());
      groups.push(group);
    }
    groups.sort((a, b) => b.length - a.length);

    return groups.flatMap((group, groupIdx) =>
      group.map((campaign) => ({ campaign, groupIdx, hasOverlap: group.length > 1 }))
    );
  }, [data, overlaps]);

  // ── Overlap count per campaign ────────────────────────────────────────────

  const overlapCountMap = useMemo(() => {
    const map = new Map<number, Set<number>>();
    if (!overlaps) return new Map<number, number>();
    for (const row of overlaps) {
      const all = [row.winningCampaign.id, ...row.losingCampaigns.map((c) => c.id)];
      for (const id of all) {
        if (!map.has(id)) map.set(id, new Set());
        for (const otherId of all) {
          if (otherId !== id) map.get(id)!.add(otherId);
        }
      }
    }
    const result = new Map<number, number>();
    for (const [id, others] of map) result.set(id, others.size);
    return result;
  }, [overlaps]);

  // ── Positioning helpers ───────────────────────────────────────────────────

  function xForDate(d: Date): number {
    const delta = d.getTime() - windowStart.getTime();
    return LEFT_GUTTER + (delta / (WINDOW_DAYS * DAY_MS)) * chartWidth;
  }

  function clampDate(d: Date): Date {
    if (d.getTime() < windowStart.getTime()) return windowStart;
    if (d.getTime() > windowEnd.getTime()) return windowEnd;
    return d;
  }

  // ── Today marker ─────────────────────────────────────────────────────────

  const today = startOfDay(new Date());
  const todayX = xForDate(today);
  const showToday = today >= windowStart && today <= windowEnd;

  // ── Day ticks for header ──────────────────────────────────────────────────

  const dayTicks: { x: number; label: string; isMonthStart: boolean }[] = [];

  for (let i = 0; i <= WINDOW_DAYS; i++) {
    const d = shiftDays(windowStart, i);
    const x = LEFT_GUTTER + (i / WINDOW_DAYS) * chartWidth;
    const isMonthStart = d.getDate() === 1;
    const everyN = chartWidth / WINDOW_DAYS > 40 ? 1 : chartWidth / WINDOW_DAYS > 20 ? 3 : 5;
    if (isMonthStart || d.getDate() % everyN === 0) {
      dayTicks.push({
        x,
        label: isMonthStart
          ? d.toLocaleDateString("en-US", { month: "short", year: "numeric" })
          : String(d.getDate()),
        isMonthStart,
      });
    }
  }

  const svgHeight = HEADER_HEIGHT + Math.max(rows.length, 1) * ROW_HEIGHT + 4;

  function goToToday() {
    setWindowStart(shiftDays(startOfDay(new Date()), -Math.floor(WINDOW_DAYS / 2)));
  }

  // ── Render ────────────────────────────────────────────────────────────────

  return (
    <TooltipProvider delayDuration={150}>
      <div ref={containerRef} className="flex flex-col flex-1 min-h-0 gap-4">
        {/* Controls row */}
        <div className="flex items-center gap-2 flex-wrap">
          <div className="flex items-center gap-1 border rounded-lg p-0.5 bg-muted/40">
            <Button
              variant="ghost"
              size="sm"
              className="h-7 px-2"
              onClick={() => setWindowStart(shiftDays(windowStart, -WINDOW_DAYS))}
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>
            <Button variant="ghost" size="sm" className="h-7 px-2.5 text-xs" onClick={goToToday}>
              Today
            </Button>
            <Button
              variant="ghost"
              size="sm"
              className="h-7 px-2"
              onClick={() => setWindowStart(shiftDays(windowStart, WINDOW_DAYS))}
            >
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
          <p className="text-sm font-medium text-muted-foreground">
            {fmtDateShort(windowStart)} – {fmtDate(shiftDays(windowEnd, -1))}
          </p>
          <p className="ml-auto text-xs text-muted-foreground">
            {rows.length} campaign{rows.length !== 1 ? "s" : ""} in range
          </p>
        </div>

        {/* Chart panel */}
        <div className="flex-1 min-h-[320px] overflow-auto border rounded-xl bg-card">
          {isLoading ? (
            <Skeleton className="h-[360px] w-full rounded-xl" />
          ) : error ? (
            <div className="flex items-center justify-center h-[360px]">
              <p className="text-sm text-muted-foreground">{getErrorMessage(error)}</p>
            </div>
          ) : rows.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-[360px] gap-3 p-12">
              <CalendarRange className="h-10 w-10 text-muted-foreground/40" />
              <p className="text-sm text-muted-foreground text-center">
                No campaigns in this date range.
              </p>
              <Button variant="outline" size="sm" onClick={goToToday}>
                Go to today
              </Button>
            </div>
          ) : (
            <svg
              width={LEFT_GUTTER + chartWidth + 32}
              height={svgHeight}
              className="block select-none"
              style={{ fontFamily: "inherit" }}
            >
              <defs>
                <pattern
                  id="tl-disabled-stripes"
                  patternUnits="userSpaceOnUse"
                  width="8"
                  height="8"
                  patternTransform="rotate(45)"
                >
                  <rect width="4" height="8" fill="rgba(0,0,0,0.2)" />
                </pattern>
              </defs>

              {/* Header background */}
              <rect
                x={0}
                y={0}
                width={LEFT_GUTTER + chartWidth + 32}
                height={HEADER_HEIGHT}
                fill="rgba(0,0,0,0.02)"
              />

              {/* Bottom border of header */}
              <line
                x1={0}
                y1={HEADER_HEIGHT}
                x2={LEFT_GUTTER + chartWidth + 32}
                y2={HEADER_HEIGHT}
                stroke="rgba(0,0,0,0.1)"
                strokeWidth={1}
              />

              {/* Left gutter right border */}
              <line
                x1={LEFT_GUTTER}
                y1={0}
                x2={LEFT_GUTTER}
                y2={svgHeight}
                stroke="rgba(0,0,0,0.08)"
                strokeWidth={1}
              />

              {/* Day grid lines + ticks */}
              {dayTicks.map((tick, i) => (
                <g key={i}>
                  <line
                    x1={tick.x}
                    y1={HEADER_HEIGHT}
                    x2={tick.x}
                    y2={svgHeight}
                    stroke="rgba(0,0,0,0.06)"
                    strokeWidth={tick.isMonthStart ? 1.5 : 0.75}
                  />
                  <text
                    x={tick.x + 4}
                    y={tick.isMonthStart ? HEADER_HEIGHT - 28 : HEADER_HEIGHT - 10}
                    fontSize={tick.isMonthStart ? 11 : 10}
                    fontWeight={tick.isMonthStart ? "600" : "400"}
                    fill={tick.isMonthStart ? "rgba(0,0,0,0.75)" : "rgba(0,0,0,0.4)"}
                  >
                    {tick.label}
                  </text>
                  {tick.isMonthStart && (
                    <line
                      x1={tick.x}
                      y1={HEADER_HEIGHT - 20}
                      x2={tick.x}
                      y2={HEADER_HEIGHT}
                      stroke="rgba(0,0,0,0.2)"
                      strokeWidth={1}
                    />
                  )}
                </g>
              ))}

              {/* Today marker */}
              {showToday && (
                <line
                  x1={todayX}
                  y1={0}
                  x2={todayX}
                  y2={svgHeight}
                  stroke="#ef4444"
                  strokeWidth={1.5}
                  strokeDasharray="4,3"
                  opacity={0.7}
                />
              )}

              {/* Rows */}
              {rows.map(({ campaign, groupIdx, hasOverlap }, rowIdx) => {
                const y = HEADER_HEIGHT + rowIdx * ROW_HEIGHT;
                const isAltGroup = groupIdx % 2 === 1 && hasOverlap;
                const status = getStatus(campaign);
                const isDisabled = status === "disabled";
                const isExpired = status === "expired";

                const fromDate = new Date(campaign.validFrom);
                const toDate = new Date(campaign.validUpto);
                const clampedFrom = clampDate(fromDate);
                const clampedTo = clampDate(toDate);
                const overflowsLeft = fromDate.getTime() < windowStart.getTime();
                const overflowsRight = toDate.getTime() > windowEnd.getTime();

                const barX = xForDate(clampedFrom);
                const barEndX = xForDate(clampedTo);
                const barW = Math.max(barEndX - barX, 4);
                const barY = y + 8;
                const barH = ROW_HEIGHT - 16;

                const fillColor = `#${campaign.colorHex}`;
                const barOpacity = isDisabled ? 0.45 : isExpired ? 0.6 : 1;
                const overlapsCount = overlapCountMap.get(campaign.id) ?? 0;

                const labelColor = isDisabled || isExpired
                  ? "rgba(0,0,0,0.38)"
                  : "rgba(0,0,0,0.75)";

                return (
                  <g key={campaign.id}>
                    {/* Row background tint for overlap groups */}
                    {isAltGroup && (
                      <rect
                        x={0}
                        y={y}
                        width={LEFT_GUTTER + chartWidth + 32}
                        height={ROW_HEIGHT}
                        fill="rgba(0,0,0,0.025)"
                      />
                    )}

                    {/* Row bottom separator */}
                    <line
                      x1={0}
                      y1={y + ROW_HEIGHT}
                      x2={LEFT_GUTTER + chartWidth + 32}
                      y2={y + ROW_HEIGHT}
                      stroke="rgba(0,0,0,0.06)"
                      strokeWidth={0.5}
                    />

                    {/* Group separator — thick line between swim-lane groups */}
                    {rowIdx > 0 &&
                      rows[rowIdx - 1].groupIdx !== groupIdx && (
                        <line
                          x1={0}
                          y1={y}
                          x2={LEFT_GUTTER + chartWidth + 32}
                          y2={y}
                          stroke="rgba(0,0,0,0.15)"
                          strokeWidth={1.5}
                        />
                      )}

                    {/* Campaign title label */}
                    <text
                      x={10}
                      y={y + ROW_HEIGHT / 2 + 1}
                      dominantBaseline="middle"
                      fontSize={12}
                      fontWeight="500"
                      fill={labelColor}
                      style={{ cursor: "pointer" }}
                      onClick={() => onEdit(campaign)}
                    >
                      {campaign.title.length > 24
                        ? campaign.title.slice(0, 22) + "…"
                        : campaign.title}
                    </text>

                    {/* SKU count */}
                    <text
                      x={LEFT_GUTTER - 8}
                      y={y + ROW_HEIGHT / 2 + 1}
                      dominantBaseline="middle"
                      textAnchor="end"
                      fontSize={10}
                      fill="rgba(0,0,0,0.38)"
                    >
                      {campaign.itemCodes.length}
                    </text>

                    {/* Gantt bar */}
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <g
                          onClick={() => onEdit(campaign)}
                          style={{ cursor: "pointer" }}
                          opacity={barOpacity}
                          role="button"
                          aria-label={campaign.title}
                        >
                          <rect
                            x={barX}
                            y={barY}
                            width={barW}
                            height={barH}
                            rx={4}
                            fill={fillColor}
                          />
                          {isDisabled && (
                            <rect
                              x={barX}
                              y={barY}
                              width={barW}
                              height={barH}
                              rx={4}
                              fill="url(#tl-disabled-stripes)"
                            />
                          )}
                          {/* Left overflow cap */}
                          {overflowsLeft && barW >= 10 && (
                            <text
                              x={barX + 5}
                              y={barY + barH / 2 + 1}
                              dominantBaseline="middle"
                              fontSize={11}
                              fontWeight="bold"
                              fill="rgba(255,255,255,0.9)"
                              style={{ pointerEvents: "none" }}
                            >
                              ‹
                            </text>
                          )}
                          {/* Right overflow cap */}
                          {overflowsRight && barW >= 10 && (
                            <text
                              x={barX + barW - 8}
                              y={barY + barH / 2 + 1}
                              dominantBaseline="middle"
                              fontSize={11}
                              fontWeight="bold"
                              fill="rgba(255,255,255,0.9)"
                              style={{ pointerEvents: "none" }}
                            >
                              ›
                            </text>
                          )}
                          {/* Overlap dot */}
                          {overlapsCount > 0 && barW >= 16 && (
                            <circle
                              cx={barX + barW - 7}
                              cy={barY + 6}
                              r={4}
                              fill="#f59e0b"
                              stroke="white"
                              strokeWidth={1}
                            />
                          )}
                          {/* Invisible expanded hit area */}
                          <rect
                            x={barX}
                            y={y}
                            width={Math.max(barW, 8)}
                            height={ROW_HEIGHT}
                            fill="transparent"
                          />
                        </g>
                      </TooltipTrigger>

                      <TooltipContent
                        side="top"
                        className="bg-popover text-popover-foreground border shadow-md p-3 max-w-[280px] space-y-1.5"
                      >
                        <div className="flex items-center gap-2">
                          <span
                            className="inline-block h-3 w-3 rounded-full shrink-0"
                            style={{ backgroundColor: fillColor }}
                          />
                          <span className="font-semibold text-sm leading-tight">
                            {campaign.title}
                          </span>
                        </div>
                        <p className="text-xs text-muted-foreground capitalize">{status}</p>
                        <p className="text-xs">
                          {fmtDate(fromDate)} – {fmtDate(toDate)}
                        </p>
                        <p className="text-xs text-muted-foreground">
                          {campaign.itemCodes.length} SKU
                          {campaign.itemCodes.length !== 1 ? "s" : ""} · −{campaign.discountValue}%
                        </p>
                        {overlapsCount > 0 && (
                          <p className="text-xs text-amber-600 flex items-center gap-1">
                            <AlertTriangle className="h-3 w-3 shrink-0" />
                            Shares SKUs with {overlapsCount} other campaign
                            {overlapsCount !== 1 ? "s" : ""}
                          </p>
                        )}
                      </TooltipContent>
                    </Tooltip>
                  </g>
                );
              })}
            </svg>
          )}
        </div>
      </div>
    </TooltipProvider>
  );
}
