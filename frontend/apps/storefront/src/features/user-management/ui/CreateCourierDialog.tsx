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
import { createCourier } from "../api";
import { getErrorMessage } from "@/shared/lib";
import { toast } from "sonner";
import { ChevronDown, ChevronUp } from "lucide-react";

const VEHICLE_TYPES = [
  { value: "BICYCLE", label: "Bicycle" },
  { value: "MOTORCYCLE", label: "Motorcycle" },
  { value: "CAR", label: "Car" },
  { value: "VAN", label: "Van" },
];

const DUPLICATE_PHONE_MSG =
  "This phone number is already registered. Find the user on the Customers tab and update their role.";

interface Props {
  open: boolean;
  onClose: () => void;
}

export function CreateCourierDialog({ open, onClose }: Props) {
  const qc = useQueryClient();

  const [phone, setPhone] = useState("");
  const [name, setName] = useState("");
  const [vehicleType, setVehicleType] = useState("");
  const [maxPayloadKg, setMaxPayloadKg] = useState("");
  const [maxLengthCm, setMaxLengthCm] = useState("");
  const [maxWidthCm, setMaxWidthCm] = useState("");
  const [maxHeightCm, setMaxHeightCm] = useState("");
  const [showDimensions, setShowDimensions] = useState(false);
  const [phoneError, setPhoneError] = useState<string | null>(null);

  const mutation = useMutation({
    mutationFn: createCourier,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["admin-users", "couriers"] });
      toast.success("Courier created");
      handleClose();
    },
    onError: (err: unknown) => {
      const status = (err as { response?: { status?: number } })?.response?.status;
      if (status === 409) {
        setPhoneError(DUPLICATE_PHONE_MSG);
      } else {
        toast.error(getErrorMessage(err));
      }
    },
  });

  function handleClose() {
    setPhone(""); setName(""); setVehicleType(""); setMaxPayloadKg("");
    setMaxLengthCm(""); setMaxWidthCm(""); setMaxHeightCm("");
    setPhoneError(null); setShowDimensions(false);
    onClose();
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setPhoneError(null);
    mutation.mutate({
      phone,
      name,
      vehicleType,
      maxPayloadKg: parseFloat(maxPayloadKg),
      maxLengthCm: maxLengthCm ? parseInt(maxLengthCm) : null,
      maxWidthCm: maxWidthCm ? parseInt(maxWidthCm) : null,
      maxHeightCm: maxHeightCm ? parseInt(maxHeightCm) : null,
    });
  }

  const isValid = phone.trim() && name.trim() && vehicleType && parseFloat(maxPayloadKg) > 0;

  return (
    <Dialog open={open} onOpenChange={(o) => { if (!o) handleClose(); }}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Add Courier</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-5">
          <div className="space-y-2">
            <Label>Phone *</Label>
            <Input
              value={phone}
              onChange={(e) => { setPhone(e.target.value); setPhoneError(null); }}
              placeholder="+992901234567"
            />
            {phoneError && (
              <p className="text-sm text-destructive">{phoneError}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label>Full Name *</Label>
            <Input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Ali Courier"
            />
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
              placeholder="30.0"
            />
          </div>

          <div className="bg-muted/30 rounded-xl p-4 space-y-3">
            <button
              type="button"
              className="flex items-center gap-2 text-sm font-medium text-foreground w-full"
              onClick={() => setShowDimensions((v) => !v)}
            >
              {showDimensions ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
              Dimension Limits (optional)
            </button>
            {showDimensions && (
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
            )}
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <Button type="button" variant="outline" onClick={handleClose}>Cancel</Button>
            <Button type="submit" disabled={!isValid || mutation.isPending}>
              {mutation.isPending ? "Creating…" : "Create Courier"}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
