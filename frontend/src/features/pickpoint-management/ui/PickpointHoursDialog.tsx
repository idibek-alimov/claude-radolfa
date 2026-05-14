"use client";

import { useEffect, useState } from "react";
import { toast } from "sonner";
import {
  usePickpointHours,
  useUpdatePickpointHours,
  useUpdatePickpoint,
  type Pickpoint,
  type UpsertPickpointHoursPayload,
} from "@/entities/pickpoint";
import { getErrorMessage } from "@/shared/lib";
import { Button } from "@/shared/ui/button";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui/dialog";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import { Switch } from "@/shared/ui/switch";

const DAY_LABELS = [
  "Monday",
  "Tuesday",
  "Wednesday",
  "Thursday",
  "Friday",
  "Saturday",
  "Sunday",
];

const TIMEZONES = [
  "UTC",
  "Asia/Dushanbe",
  "Asia/Tashkent",
  "Asia/Almaty",
  "Europe/Moscow",
  "Europe/London",
  "America/New_York",
];

interface DayState {
  dayOfWeek: number;
  enabled: boolean;
  openTime: string;
  closeTime: string;
  error?: string;
}

interface PickpointHoursDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  pickpoint: Pickpoint;
}

export function PickpointHoursDialog({
  open,
  onOpenChange,
  pickpoint,
}: PickpointHoursDialogProps) {
  const [days, setDays] = useState<DayState[]>(() =>
    DAY_LABELS.map((_, i) => ({
      dayOfWeek: i + 1,
      enabled: false,
      openTime: "09:00",
      closeTime: "18:00",
    })),
  );
  const [timezone, setTimezone] = useState(pickpoint.timezone ?? "Asia/Dushanbe");
  const [temporarilyClosed, setTemporarilyClosed] = useState(pickpoint.temporarilyClosed);

  const { data: hoursData } = usePickpointHours(open ? pickpoint.id : null);
  const hoursMutation = useUpdatePickpointHours();
  const pickpointMutation = useUpdatePickpoint();
  const isPending = hoursMutation.isPending || pickpointMutation.isPending;

  useEffect(() => {
    if (open) {
      const serverDays = hoursData ?? [];
      setDays(
        DAY_LABELS.map((_, i) => {
          const dow = i + 1;
          const existing = serverDays.find((h) => h.dayOfWeek === dow);
          return {
            dayOfWeek: dow,
            enabled: !!existing,
            openTime: existing?.openTime ?? "09:00",
            closeTime: existing?.closeTime ?? "18:00",
          };
        }),
      );
      setTimezone(pickpoint.timezone ?? "Asia/Dushanbe");
      setTemporarilyClosed(pickpoint.temporarilyClosed);
    }
  }, [open, hoursData, pickpoint]);

  function updateDay(index: number, patch: Partial<DayState>) {
    setDays((prev) =>
      prev.map((d, i) => (i === index ? { ...d, ...patch, error: undefined } : d)),
    );
  }

  async function handleSubmit() {
    // Validate: closeTime must be after openTime for enabled days
    let hasError = false;
    const validated = days.map((d) => {
      if (d.enabled && d.closeTime <= d.openTime) {
        hasError = true;
        return { ...d, error: "Close time must be after open time" };
      }
      return d;
    });
    if (hasError) {
      setDays(validated);
      return;
    }

    const enabledDays: UpsertPickpointHoursPayload[] = days
      .filter((d) => d.enabled)
      .map((d) => ({ dayOfWeek: d.dayOfWeek, openTime: d.openTime, closeTime: d.closeTime }));

    try {
      await hoursMutation.mutateAsync({ id: pickpoint.id, hours: enabledDays });
      await pickpointMutation.mutateAsync({
        id: pickpoint.id,
        payload: {
          name: pickpoint.name,
          address: pickpoint.address,
          active: pickpoint.active,
          latitude: pickpoint.latitude,
          longitude: pickpoint.longitude,
          hasParking: pickpoint.hasParking,
          hasFittingRoom: pickpoint.hasFittingRoom,
          hasCardPayment: pickpoint.hasCardPayment,
          wheelchairAccessible: pickpoint.wheelchairAccessible,
          timezone,
          temporarilyClosed,
        },
      });
      toast.success("Hours updated");
      onOpenChange(false);
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Opening Hours — {pickpoint.name}</DialogTitle>
        </DialogHeader>

        <div className="space-y-4 py-1">
          {/* 7-day schedule */}
          <div className="rounded-lg border divide-y">
            {days.map((day, index) => (
              <div key={day.dayOfWeek}>
                <div className="flex items-center justify-between px-3 py-2">
                  <span className="text-sm font-medium w-28">
                    {DAY_LABELS[index]}
                  </span>
                  {day.enabled ? (
                    <div className="flex items-center gap-2 flex-1 mx-3">
                      <Input
                        type="time"
                        value={day.openTime}
                        onChange={(e) => updateDay(index, { openTime: e.target.value })}
                        disabled={isPending}
                        className="h-8 w-28"
                      />
                      <span className="text-xs text-muted-foreground">to</span>
                      <Input
                        type="time"
                        value={day.closeTime}
                        onChange={(e) => updateDay(index, { closeTime: e.target.value })}
                        disabled={isPending}
                        className="h-8 w-28"
                      />
                    </div>
                  ) : (
                    <span className="text-xs text-muted-foreground flex-1 mx-3">Closed</span>
                  )}
                  <Switch
                    checked={day.enabled}
                    onCheckedChange={(checked) => updateDay(index, { enabled: checked })}
                    disabled={isPending}
                  />
                </div>
                {day.error && (
                  <p className="text-xs text-destructive px-3 pb-2">{day.error}</p>
                )}
              </div>
            ))}
          </div>

          {/* Timezone */}
          <div className="space-y-1">
            <Label>Timezone</Label>
            <Select value={timezone} onValueChange={setTimezone} disabled={isPending}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {TIMEZONES.map((tz) => (
                  <SelectItem key={tz} value={tz}>
                    {tz}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Temporarily closed */}
          <div className="flex items-center justify-between rounded-lg border p-3">
            <div className="space-y-0.5">
              <Label>Temporarily closed</Label>
              <p className="text-xs text-muted-foreground">
                Overrides the schedule immediately.
              </p>
            </div>
            <Switch
              checked={temporarilyClosed}
              onCheckedChange={setTemporarilyClosed}
              disabled={isPending}
            />
          </div>
        </div>

        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={isPending}
          >
            Cancel
          </Button>
          <Button type="button" onClick={handleSubmit} disabled={isPending}>
            {isPending ? "Saving…" : "Save Hours"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
