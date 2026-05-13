"use client";

import { useEffect, useState } from "react";
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
  const [errors, setErrors] = useState<{ name?: string; address?: string }>({});

  useEffect(() => {
    if (open) {
      setName(initialData?.name ?? "");
      setAddress(initialData?.address ?? "");
      setActive(initialData?.active ?? true);
      setErrors({});
    }
  }, [open, initialData]);

  const createMutation = useCreatePickpoint();
  const updateMutation = useUpdatePickpoint();
  const isPending = createMutation.isPending || updateMutation.isPending;

  function handleSubmit() {
    const trimmedName = name.trim();
    const trimmedAddress = address.trim();

    const nextErrors: { name?: string; address?: string } = {};
    if (!trimmedName) {
      nextErrors.name = "Name is required";
    } else if (trimmedName.length > 255) {
      nextErrors.name = "Name must not exceed 255 characters";
    }
    if (!trimmedAddress) {
      nextErrors.address = "Address is required";
    }

    if (nextErrors.name || nextErrors.address) {
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

    if (isEdit && initialData) {
      updateMutation.mutate(
        { id: initialData.id, payload: { name: trimmedName, address: trimmedAddress, active } },
        handlers,
      );
    } else {
      createMutation.mutate({ name: trimmedName, address: trimmedAddress }, handlers);
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg">
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
