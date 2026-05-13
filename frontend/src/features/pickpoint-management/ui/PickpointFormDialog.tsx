"use client";

import { useEffect, useState } from "react";
import { Accessibility, Car, CreditCard, Shirt } from "lucide-react";
import { toast } from "sonner";
import {
  useCreatePickpoint,
  useUpdatePickpoint,
  type Pickpoint,
} from "@/entities/pickpoint";
import { getErrorMessage } from "@/shared/lib";
import { Button } from "@/shared/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui/dialog";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Switch } from "@/shared/ui/switch";
import { Textarea } from "@/shared/ui/textarea";

interface PickpointFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  initialData?: Pickpoint;
}

export function PickpointFormDialog({
  open,
  onOpenChange,
  initialData,
}: PickpointFormDialogProps) {
  const isEdit = !!initialData;

  const [name, setName] = useState("");
  const [address, setAddress] = useState("");
  const [active, setActive] = useState(true);
  const [latitude, setLatitude] = useState<string>("");
  const [longitude, setLongitude] = useState<string>("");
  const [hasParking, setHasParking] = useState(false);
  const [hasFittingRoom, setHasFittingRoom] = useState(false);
  const [hasCardPayment, setHasCardPayment] = useState(false);
  const [wheelchairAccessible, setWheelchairAccessible] = useState(false);
  const [errors, setErrors] = useState<{
    name?: string;
    address?: string;
    latitude?: string;
    longitude?: string;
  }>({});

  useEffect(() => {
    if (open) {
      setName(initialData?.name ?? "");
      setAddress(initialData?.address ?? "");
      setActive(initialData?.active ?? true);
      setLatitude(initialData?.latitude?.toString() ?? "");
      setLongitude(initialData?.longitude?.toString() ?? "");
      setHasParking(initialData?.hasParking ?? false);
      setHasFittingRoom(initialData?.hasFittingRoom ?? false);
      setHasCardPayment(initialData?.hasCardPayment ?? false);
      setWheelchairAccessible(initialData?.wheelchairAccessible ?? false);
      setErrors({});
    }
  }, [open, initialData]);

  const createMutation = useCreatePickpoint();
  const updateMutation = useUpdatePickpoint();
  const isPending = createMutation.isPending || updateMutation.isPending;

  function handleSubmit() {
    const trimmedName = name.trim();
    const trimmedAddress = address.trim();

    const nextErrors: {
      name?: string;
      address?: string;
      latitude?: string;
      longitude?: string;
    } = {};
    if (!trimmedName) {
      nextErrors.name = "Name is required";
    } else if (trimmedName.length > 255) {
      nextErrors.name = "Name must not exceed 255 characters";
    }
    if (!trimmedAddress) {
      nextErrors.address = "Address is required";
    }

    let parsedLat: number | null = null;
    let parsedLng: number | null = null;
    if (latitude.trim() !== "") {
      const v = Number(latitude);
      if (!Number.isFinite(v) || v < -90 || v > 90) {
        nextErrors.latitude = "Latitude must be between -90 and 90";
      } else {
        parsedLat = v;
      }
    }
    if (longitude.trim() !== "") {
      const v = Number(longitude);
      if (!Number.isFinite(v) || v < -180 || v > 180) {
        nextErrors.longitude = "Longitude must be between -180 and 180";
      } else {
        parsedLng = v;
      }
    }

    if (nextErrors.name || nextErrors.address || nextErrors.latitude || nextErrors.longitude) {
      setErrors(nextErrors);
      return;
    }

    const handlers = {
      onSuccess: () => {
        toast.success(isEdit ? "Pickpoint updated" : "Pickpoint created");
        onOpenChange(false);
      },
      onError: (err: unknown) => toast.error(getErrorMessage(err)),
    };

    const amenities = {
      latitude: parsedLat,
      longitude: parsedLng,
      hasParking,
      hasFittingRoom,
      hasCardPayment,
      wheelchairAccessible,
    };

    if (isEdit && initialData) {
      updateMutation.mutate(
        { id: initialData.id, payload: { name: trimmedName, address: trimmedAddress, active, ...amenities } },
        handlers,
      );
    } else {
      createMutation.mutate({ name: trimmedName, address: trimmedAddress, ...amenities }, handlers);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>{isEdit ? "Edit Pickpoint" : "Add Pickpoint"}</DialogTitle>
          <DialogDescription>
            {isEdit
              ? "Update name, address, or availability."
              : "Create a new pickup location. New pickpoints are active by default."}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-5 py-2">
          <div className="space-y-2">
            <Label htmlFor="pickpoint-name">Name</Label>
            <Input
              id="pickpoint-name"
              maxLength={255}
              value={name}
              onChange={(e) => setName(e.target.value)}
              disabled={isPending}
              placeholder="e.g. Main Street Store"
            />
            {errors.name && (
              <p className="text-sm text-destructive">{errors.name}</p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="pickpoint-address">Address</Label>
            <Textarea
              id="pickpoint-address"
              rows={3}
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              disabled={isPending}
              placeholder="Full street address"
            />
            {errors.address && (
              <p className="text-sm text-destructive">{errors.address}</p>
            )}
          </div>

          {/* Coordinates */}
          <div className="space-y-2">
            <Label>
              Coordinates{" "}
              <span className="text-xs text-muted-foreground">(optional)</span>
            </Label>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <Label htmlFor="pickpoint-lat" className="text-xs text-muted-foreground">
                  Latitude
                </Label>
                <Input
                  id="pickpoint-lat"
                  type="number"
                  step="any"
                  placeholder="e.g. 38.5598"
                  value={latitude}
                  onChange={(e) => setLatitude(e.target.value)}
                  disabled={isPending}
                />
                {errors.latitude && (
                  <p className="text-sm text-destructive">{errors.latitude}</p>
                )}
              </div>
              <div className="space-y-1">
                <Label htmlFor="pickpoint-lng" className="text-xs text-muted-foreground">
                  Longitude
                </Label>
                <Input
                  id="pickpoint-lng"
                  type="number"
                  step="any"
                  placeholder="e.g. 68.7738"
                  value={longitude}
                  onChange={(e) => setLongitude(e.target.value)}
                  disabled={isPending}
                />
                {errors.longitude && (
                  <p className="text-sm text-destructive">{errors.longitude}</p>
                )}
              </div>
            </div>
          </div>

          {/* Amenities */}
          <div className="space-y-2">
            <Label>Amenities</Label>
            <div className="rounded-lg border divide-y">
              {([
                { id: "parking",  label: "Parking",              Icon: Car,           value: hasParking,           setter: setHasParking           },
                { id: "fitting",  label: "Fitting room",          Icon: Shirt,         value: hasFittingRoom,       setter: setHasFittingRoom       },
                { id: "card",     label: "Card payment",          Icon: CreditCard,    value: hasCardPayment,       setter: setHasCardPayment       },
                { id: "wheel",    label: "Wheelchair accessible", Icon: Accessibility, value: wheelchairAccessible, setter: setWheelchairAccessible },
              ] as const).map(({ id, label, Icon, value, setter }) => (
                <div key={id} className="flex items-center justify-between px-3 py-2">
                  <div className="flex items-center gap-2 text-sm">
                    <Icon className="h-4 w-4 text-muted-foreground" />
                    {label}
                  </div>
                  <Switch
                    checked={value}
                    onCheckedChange={setter}
                    disabled={isPending}
                  />
                </div>
              ))}
            </div>
          </div>

          {isEdit && (
            <div className="flex items-center justify-between rounded-lg border p-3">
              <div className="space-y-0.5">
                <Label htmlFor="pickpoint-active">Active</Label>
                <p className="text-xs text-muted-foreground">
                  Inactive pickpoints are hidden from checkout.
                </p>
              </div>
              <Switch
                id="pickpoint-active"
                checked={active}
                onCheckedChange={setActive}
                disabled={isPending}
              />
            </div>
          )}
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
            {isPending
              ? isEdit ? "Saving…" : "Creating…"
              : isEdit ? "Save Changes" : "Create Pickpoint"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
