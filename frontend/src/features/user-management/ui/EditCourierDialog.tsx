"use client";

import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui/dialog";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import { updateCourierDetails } from "../api";
import type { UserDto } from "../types";
import { getErrorMessage } from "@/shared/lib";
import { toast } from "sonner";

const VEHICLE_TYPES = [
  { value: "BICYCLE", label: "Bicycle" },
  { value: "MOTORCYCLE", label: "Motorcycle" },
  { value: "CAR", label: "Car" },
  { value: "VAN", label: "Van" },
];

interface Props {
  open: boolean;
  onClose: () => void;
  target: UserDto;
}

export function EditCourierDialog({ open, onClose, target }: Props) {
  const qc = useQueryClient();

  const [vehicleType, setVehicleType] = useState(target.vehicleType ?? "");
  const [maxPayloadKg, setMaxPayloadKg] = useState(String(target.maxPayloadKg ?? ""));
  const [maxLengthCm, setMaxLengthCm] = useState(String(target.maxLengthCm ?? ""));
  const [maxWidthCm, setMaxWidthCm] = useState(String(target.maxWidthCm ?? ""));
  const [maxHeightCm, setMaxHeightCm] = useState(String(target.maxHeightCm ?? ""));

  const mutation = useMutation({
    mutationFn: () => updateCourierDetails(target.id, {
      vehicleType: vehicleType as string,
      maxPayloadKg: parseFloat(maxPayloadKg),
      maxLengthCm: maxLengthCm ? parseInt(maxLengthCm) : null,
      maxWidthCm: maxWidthCm ? parseInt(maxWidthCm) : null,
      maxHeightCm: maxHeightCm ? parseInt(maxHeightCm) : null,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["admin-users", "couriers"] });
      toast.success("Courier updated");
      onClose();
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const isValid = vehicleType && parseFloat(maxPayloadKg) > 0;

  return (
    <Dialog open={open} onOpenChange={(o) => { if (!o) onClose(); }}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Edit Courier</DialogTitle>
        </DialogHeader>
        <div className="space-y-5">
          <div className="bg-muted/30 rounded-xl p-4 space-y-1">
            <p className="text-xs text-muted-foreground">Phone</p>
            <p className="text-sm font-medium">{target.phone}</p>
            {target.name && (
              <>
                <p className="text-xs text-muted-foreground pt-1">Name</p>
                <p className="text-sm font-medium">{target.name}</p>
              </>
            )}
          </div>

          <div className="space-y-2">
            <Label>Vehicle Type *</Label>
            <Select value={vehicleType} onValueChange={setVehicleType}>
              <SelectTrigger>
                <SelectValue placeholder="Select vehicle type" />
              </SelectTrigger>
              <SelectContent>
                {VEHICLE_TYPES.map((v) => (
                  <SelectItem key={v.value} value={v.value}>{v.label}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label>Max Payload (kg) *</Label>
            <Input
              type="number"
              min="0.1"
              step="0.1"
              value={maxPayloadKg}
              onChange={(e) => setMaxPayloadKg(e.target.value)}
            />
          </div>

          <div className="bg-muted/30 rounded-xl p-4 space-y-3">
            <p className="text-sm font-medium">Dimension Limits (optional)</p>
            <div className="grid grid-cols-3 gap-3">
              <div className="space-y-1">
                <Label className="text-xs">Max Length (cm)</Label>
                <Input type="number" min="1" step="1" value={maxLengthCm} onChange={(e) => setMaxLengthCm(e.target.value)} />
              </div>
              <div className="space-y-1">
                <Label className="text-xs">Max Width (cm)</Label>
                <Input type="number" min="1" step="1" value={maxWidthCm} onChange={(e) => setMaxWidthCm(e.target.value)} />
              </div>
              <div className="space-y-1">
                <Label className="text-xs">Max Height (cm)</Label>
                <Input type="number" min="1" step="1" value={maxHeightCm} onChange={(e) => setMaxHeightCm(e.target.value)} />
              </div>
            </div>
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <Button type="button" variant="outline" onClick={onClose}>Cancel</Button>
            <Button onClick={() => mutation.mutate()} disabled={!isValid || mutation.isPending}>
              {mutation.isPending ? "Saving…" : "Save Changes"}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
