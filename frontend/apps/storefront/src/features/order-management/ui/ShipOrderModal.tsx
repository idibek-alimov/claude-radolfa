"use client";

import { useState, useMemo } from "react";
import { useTranslations } from "next-intl";
import { AlertTriangle, Truck } from "lucide-react";
import { toast } from "sonner";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
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
import { useUpdateOrderStatus, useAdminOrder } from "@/entities/order";
import { useCourierList } from "@/features/fleet/api";
import { getErrorMessage } from "@/shared/lib";

const VEHICLE_LABELS: Record<string, string> = {
  BICYCLE: "Bicycle",
  MOTORCYCLE: "Motorcycle",
  CAR: "Car",
  VAN: "Van",
};

interface ShipOrderModalProps {
  open: boolean;
  onClose: () => void;
  orderId: number;
}

export function ShipOrderModal({ open, onClose, orderId }: ShipOrderModalProps) {
  const t = useTranslations("manage.orders");
  const updateStatus = useUpdateOrderStatus();
  const { data: couriers = [] } = useCourierList();
  const { data: order } = useAdminOrder(open ? orderId : null);

  const [selectedCourierId, setSelectedCourierId] = useState<number | null>(null);
  const [trackingNumber, setTrackingNumber]               = useState("");
  const [estimatedDeliveryDate, setEstimatedDeliveryDate] = useState("");
  const [courierError, setCourierError]                   = useState(false);

  const selectedCourier = useMemo(
    () => couriers.find((c) => c.id === selectedCourierId) ?? null,
    [couriers, selectedCourierId]
  );

  const orderWeight = useMemo(() => {
    if (!order) return null;
    const total = order.items.reduce((sum, item) => {
      const w = (item as { weightKg?: number | null }).weightKg ?? 0;
      return sum + w * item.quantity;
    }, 0);
    return total > 0 ? total : null;
  }, [order]);

  const capacityWarning =
    orderWeight !== null &&
    selectedCourier?.maxPayloadKg !== null &&
    selectedCourier?.maxPayloadKg !== undefined &&
    orderWeight > selectedCourier.maxPayloadKg;

  function reset() {
    setSelectedCourierId(null);
    setTrackingNumber("");
    setEstimatedDeliveryDate("");
    setCourierError(false);
  }

  function handleClose() {
    reset();
    onClose();
  }

  function handleSubmit() {
    if (!selectedCourierId) {
      setCourierError(true);
      return;
    }
    setCourierError(false);
    updateStatus.mutate(
      {
        orderId,
        status: "SHIPPED",
        courierId: selectedCourierId,
        trackingNumber,
        estimatedDeliveryDate,
      },
      {
        onSuccess: () => {
          toast.success(t("toast.shipped"));
          reset();
          onClose();
        },
        onError: (err) => toast.error(getErrorMessage(err, t("toast.shipFailed"))),
      }
    );
  }

  return (
    <Dialog open={open} onOpenChange={(o) => !o && handleClose()}>
      <DialogContent className="max-w-md">
        <DialogHeader>
          <DialogTitle>{t("drawer.markAsShipped")}</DialogTitle>
        </DialogHeader>

        <div className="space-y-4 py-2">
          {/* Courier Select */}
          <div className="space-y-1.5">
            <Label className="text-sm">
              Courier <span className="text-destructive">*</span>
            </Label>
            <Select
              value={selectedCourierId?.toString() ?? ""}
              onValueChange={(v) => {
                setSelectedCourierId(Number(v));
                setCourierError(false);
              }}
            >
              <SelectTrigger className={courierError ? "border-destructive" : ""}>
                <SelectValue placeholder="Select a courier…" />
              </SelectTrigger>
              <SelectContent>
                {couriers.map((c) => (
                  <SelectItem key={c.id} value={c.id.toString()}>
                    {c.name}
                    {c.vehicleType && (
                      <span className="ml-1 text-muted-foreground text-xs">
                        — {VEHICLE_LABELS[c.vehicleType] ?? c.vehicleType}
                      </span>
                    )}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            {courierError && (
              <p className="text-destructive text-xs">Please select a courier.</p>
            )}
          </div>

          {/* Capacity warning */}
          {capacityWarning && selectedCourier && orderWeight !== null && (
            <div className="flex items-start gap-2 rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800">
              <AlertTriangle className="h-4 w-4 shrink-0 mt-0.5" />
              <p>
                This order ({orderWeight.toFixed(2)} kg) exceeds this courier&apos;s max
                payload ({selectedCourier.maxPayloadKg} kg). Proceed with caution.
              </p>
            </div>
          )}

          <div className="space-y-1.5">
            <Label htmlFor="tracking-number" className="text-sm">{t("drawer.trackingNumber")}</Label>
            <Input
              id="tracking-number"
              value={trackingNumber}
              onChange={(e) => setTrackingNumber(e.target.value)}
              placeholder={t("drawer.trackingNumberPlaceholder")}
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="edd" className="text-sm">{t("drawer.estimatedDeliveryDate")}</Label>
            <Input
              id="edd"
              type="date"
              value={estimatedDeliveryDate}
              onChange={(e) => setEstimatedDeliveryDate(e.target.value)}
            />
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={handleClose} disabled={updateStatus.isPending}>
            {t("shipModal.cancel")}
          </Button>
          <Button onClick={handleSubmit} disabled={updateStatus.isPending}>
            <Truck className="h-4 w-4 mr-2" />
            {updateStatus.isPending ? t("shipModal.shipping") : t("drawer.markAsShipped")}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
