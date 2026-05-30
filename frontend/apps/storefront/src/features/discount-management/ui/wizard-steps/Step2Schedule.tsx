"use client";

import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { QuickDateRange } from "../QuickDateRange";
import type { DiscountWizardState } from "../DiscountCreationWizard";
import { CalendarDays, Clock } from "lucide-react";

function SectionHeading({
  icon: Icon,
  title,
}: {
  icon: React.ElementType;
  title: string;
}) {
  return (
    <div className="flex items-center gap-2 pb-2 border-b border-border">
      <Icon className="h-4 w-4 text-muted-foreground" />
      <h3 className="text-sm font-semibold text-foreground">{title}</h3>
    </div>
  );
}

function getDurationLabel(from: string, to: string): string | null {
  if (!from || !to) return null;
  const diff = new Date(to).getTime() - new Date(from).getTime();
  if (diff <= 0) return null;
  const days = Math.round(diff / (1000 * 60 * 60 * 24));
  if (days === 1) return "1 day";
  return `${days} days`;
}

function formatDateTime(local: string): string {
  if (!local) return "";
  const d = new Date(local);
  return d.toLocaleString(undefined, {
    day: "2-digit",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

interface Props {
  state: DiscountWizardState;
  update: (patch: Partial<DiscountWizardState>) => void;
  submitted: boolean;
}

export function Step2Schedule({ state, update, submitted }: Props) {
  const durationLabel = getDurationLabel(state.validFrom, state.validUpto);

  const fromError = submitted && !state.validFrom;
  const toError = submitted && !state.validUpto;
  const rangeError =
    submitted &&
    state.validFrom &&
    state.validUpto &&
    new Date(state.validUpto) <= new Date(state.validFrom);

  return (
    <div className="flex-1 flex flex-col space-y-10">
      {/* Heading */}
      <div>
        <h2 className="text-xl font-semibold tracking-tight">Schedule</h2>
        <p className="text-sm text-muted-foreground mt-1">
          Set when this discount campaign starts and ends.
        </p>
      </div>

      {/* Date Range section */}
      <section className="space-y-6">
        <SectionHeading icon={CalendarDays} title="Date Range" />

        <div className="rounded-xl border border-border bg-muted/30 p-6 space-y-6">
          <div className="grid grid-cols-2 gap-6">
            <div className="space-y-1.5">
              <Label htmlFor="validFrom">
                Valid from <span className="text-destructive">*</span>
              </Label>
              <Input
                id="validFrom"
                type="datetime-local"
                value={state.validFrom}
                onChange={(e) => update({ validFrom: e.target.value })}
                className={`h-11 ${fromError ? "border-destructive focus-visible:ring-destructive" : ""}`}
              />
              {fromError && (
                <p className="text-xs text-destructive">Start date is required.</p>
              )}
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="validUpto">
                Valid until <span className="text-destructive">*</span>
              </Label>
              <Input
                id="validUpto"
                type="datetime-local"
                value={state.validUpto}
                onChange={(e) => update({ validUpto: e.target.value })}
                className={`h-11 ${toError ? "border-destructive focus-visible:ring-destructive" : ""}`}
              />
              {toError && (
                <p className="text-xs text-destructive">End date is required.</p>
              )}
            </div>
          </div>

          {rangeError && (
            <p className="text-xs text-destructive">
              End date must be after start date.
            </p>
          )}

          {/* Quick presets */}
          <div className="space-y-2">
            <p className="text-xs text-muted-foreground font-medium">Quick presets</p>
            <QuickDateRange
              onSelect={(from, to) => update({ validFrom: from, validUpto: to })}
            />
          </div>
        </div>
      </section>

      {/* Duration — prominent info card */}
      {durationLabel && (
        <div className="rounded-xl bg-primary/10 border border-primary/20 p-6 flex items-center gap-8">
          {/* Duration */}
          <div className="flex items-center gap-4">
            <div className="h-11 w-11 rounded-full bg-primary/20 flex items-center justify-center shrink-0">
              <Clock className="h-5 w-5 text-primary" />
            </div>
            <div>
              <p className="text-xs font-medium text-primary/70 uppercase tracking-wide">
                Duration
              </p>
              <p className="text-2xl font-bold text-primary tabular-nums">
                {durationLabel}
              </p>
            </div>
          </div>

          {/* Divider */}
          <div className="h-10 w-px bg-primary/20 shrink-0" />

          {/* Period */}
          <div>
            <p className="text-xs font-medium text-primary/70 uppercase tracking-wide mb-1">
              Period
            </p>
            <p className="text-sm font-semibold text-primary">
              {formatDateTime(state.validFrom)} → {formatDateTime(state.validUpto)}
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
