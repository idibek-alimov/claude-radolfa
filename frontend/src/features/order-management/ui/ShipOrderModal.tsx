"use client";

import { useState } from "react";
import { useTranslations } from "next-intl";
import { Truck } from "lucide-react";
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
import { useUpdateOrderStatus } from "@/entities/order";
import { getErrorMessage } from "@/shared/lib";

interface ShipOrderModalProps {
  open: boolean;
  onClose: () => void;
  orderId: number;
}

export function ShipOrderModal({ open, onClose, orderId }: ShipOrderModalProps) {
  const t = useTranslations("manage.orders");
  const updateStatus = useUpdateOrderStatus();

  const [courierName, setCourierName]                     = useState("");
  const [trackingNumber, setTrackingNumber]               = useState("");
  const [estimatedDeliveryDate, setEstimatedDeliveryDate] = useState("");
  const [courierError, setCourierError]                   = useState(false);

  function reset() {
    setCourierName("");
    setTrackingNumber("");
    setEstimatedDeliveryDate("");
    setCourierError(false);
  }

  function handleClose() {
    reset();
    onClose();
  }

  function handleSubmit() {
    if (!courierName.trim()) {
      setCourierError(true);
      return;
    }
    setCourierError(false);
    updateStatus.mutate(
      {
        orderId,
        status: "SHIPPED",
        courierName,
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
          <div className="space-y-1.5">
            <Label htmlFor="courier-name" className="text-sm">
              {t("drawer.courierName")} <span className="text-destructive">*</span>
            </Label>
            <Input
              id="courier-name"
              value={courierName}
              onChange={(e) => { setCourierName(e.target.value); setCourierError(false); }}
              placeholder={t("drawer.courierNamePlaceholder")}
              className={courierError ? "border-destructive" : ""}
            />
            {courierError && (
              <p className="text-destructive text-xs">{t("drawer.courierNameRequired")}</p>
            )}
          </div>

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
