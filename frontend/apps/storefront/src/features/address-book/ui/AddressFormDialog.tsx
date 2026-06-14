"use client";

import { useEffect, useState } from "react";
import { toast } from "sonner";
import { useTranslations } from "next-intl";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@radolfa/shared/ui/dialog";
import { Button } from "@radolfa/shared/ui/button";
import { Input } from "@radolfa/shared/ui/input";
import { Label } from "@radolfa/shared/ui/label";
import { Switch } from "@radolfa/shared/ui/switch";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@radolfa/shared/ui/select";
import { getErrorMessage } from "@radolfa/shared/lib";
import {
  useCreateAddress,
  useUpdateAddress,
  type Address,
  type AddressLabel,
  type AddressRequest,
} from "@/entities/address";

const ADDRESS_LABEL_KEYS: Record<AddressLabel, string> = {
  HOME: "labelHome",
  WORK: "labelWork",
  OTHER: "labelOther",
};

const EMPTY_FORM: AddressRequest = {
  label: "HOME",
  recipientName: "",
  phone: "",
  line1: "",
  city: "",
  postalCode: "",
  country: "",
  isDefault: false,
};

interface AddressFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** When provided, the dialog edits this address; otherwise it creates a new one. */
  address?: Address | null;
}

export function AddressFormDialog({ open, onOpenChange, address }: AddressFormDialogProps) {
  const t = useTranslations("profile");
  const isEdit = !!address;
  const [form, setForm] = useState<AddressRequest>(EMPTY_FORM);

  const createMutation = useCreateAddress();
  const updateMutation = useUpdateAddress();
  const isPending = createMutation.isPending || updateMutation.isPending;

  // Seed the form whenever the dialog opens, for create or edit.
  useEffect(() => {
    if (!open) return;
    setForm(
      address
        ? {
            label: address.label,
            recipientName: address.recipientName,
            phone: address.phone,
            line1: address.line1,
            city: address.city,
            postalCode: address.postalCode ?? "",
            country: address.country,
            isDefault: address.isDefault,
          }
        : EMPTY_FORM
    );
  }, [open, address]);

  const isValid =
    form.recipientName.trim() &&
    form.phone.trim() &&
    form.line1.trim() &&
    form.city.trim() &&
    form.country.trim();

  function handleSubmit() {
    const payload: AddressRequest = {
      ...form,
      postalCode: form.postalCode?.trim() ? form.postalCode.trim() : null,
    };

    if (isEdit && address) {
      // isDefault is managed exclusively via PATCH /addresses/{id}/default,
      // so the edit payload always echoes the address's current value unchanged.
      updateMutation.mutate(
        { id: address.id, payload: { ...payload, isDefault: address.isDefault } },
        {
          onSuccess: () => {
            onOpenChange(false);
            toast.success(t("addressSaved"));
          },
          onError: (err) => toast.error(getErrorMessage(err)),
        }
      );
    } else {
      createMutation.mutate(payload, {
        onSuccess: () => {
          onOpenChange(false);
          toast.success(t("addressSaved"));
        },
        onError: (err) => toast.error(getErrorMessage(err)),
      });
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent overlayClassName="bg-black/30" className="max-w-md">
        <DialogHeader>
          <DialogTitle>{isEdit ? t("editAddress") : t("addAddress")}</DialogTitle>
        </DialogHeader>

        <div className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="address-label">{t("addressLabelField")}</Label>
            <Select
              value={form.label}
              onValueChange={(value) => setForm((f) => ({ ...f, label: value as AddressLabel }))}
            >
              <SelectTrigger id="address-label">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {(Object.keys(ADDRESS_LABEL_KEYS) as AddressLabel[]).map((label) => (
                  <SelectItem key={label} value={label}>
                    {t(ADDRESS_LABEL_KEYS[label])}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="address-recipient">{t("recipientNameLabel")}</Label>
            <Input
              id="address-recipient"
              value={form.recipientName}
              onChange={(e) => setForm((f) => ({ ...f, recipientName: e.target.value }))}
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="address-phone">{t("phoneNumber")}</Label>
            <Input
              id="address-phone"
              value={form.phone}
              onChange={(e) => setForm((f) => ({ ...f, phone: e.target.value }))}
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="address-line1">{t("line1Label")}</Label>
            <Input
              id="address-line1"
              value={form.line1}
              onChange={(e) => setForm((f) => ({ ...f, line1: e.target.value }))}
            />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="address-city">{t("cityLabel")}</Label>
              <Input
                id="address-city"
                value={form.city}
                onChange={(e) => setForm((f) => ({ ...f, city: e.target.value }))}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="address-postal">
                {t("postalCodeLabel")} <span className="text-ink/40">({t("optional")})</span>
              </Label>
              <Input
                id="address-postal"
                value={form.postalCode ?? ""}
                onChange={(e) => setForm((f) => ({ ...f, postalCode: e.target.value }))}
              />
            </div>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="address-country">{t("countryLabel")}</Label>
            <Input
              id="address-country"
              value={form.country}
              onChange={(e) => setForm((f) => ({ ...f, country: e.target.value }))}
            />
          </div>

          {!isEdit && (
            <div className="flex items-center justify-between rounded-2xl bg-soft px-4 py-3">
              <Label htmlFor="address-default" className="cursor-pointer">
                {t("setDefault")}
              </Label>
              <Switch
                id="address-default"
                checked={form.isDefault}
                onCheckedChange={(checked) => setForm((f) => ({ ...f, isDefault: checked }))}
              />
            </div>
          )}

          <Button
            className="w-full rounded-full bg-mag text-white hover:bg-maglo"
            disabled={!isValid || isPending}
            onClick={handleSubmit}
          >
            {t("saveAddress")}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
