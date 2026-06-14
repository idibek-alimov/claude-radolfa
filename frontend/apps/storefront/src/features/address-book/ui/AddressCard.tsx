"use client";

import { useState } from "react";
import { Home, Briefcase, MapPin } from "lucide-react";
import { toast } from "sonner";
import { useTranslations } from "next-intl";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@radolfa/shared/ui/alert-dialog";
import { getErrorMessage } from "@radolfa/shared/lib";
import {
  useDeleteAddress,
  useSetDefaultAddress,
  type Address,
  type AddressLabel,
} from "@/entities/address";

const ADDRESS_LABEL_KEYS: Record<AddressLabel, string> = {
  HOME: "labelHome",
  WORK: "labelWork",
  OTHER: "labelOther",
};

const ADDRESS_LABEL_ICONS: Record<AddressLabel, typeof Home> = {
  HOME: Home,
  WORK: Briefcase,
  OTHER: MapPin,
};

interface AddressCardProps {
  address: Address;
  onEdit: (address: Address) => void;
}

export function AddressCard({ address, onEdit }: AddressCardProps) {
  const t = useTranslations("profile");
  const [confirmDelete, setConfirmDelete] = useState(false);

  const setDefaultMutation = useSetDefaultAddress();
  const deleteMutation = useDeleteAddress();

  const Icon = ADDRESS_LABEL_ICONS[address.label];

  function handleSetDefault() {
    setDefaultMutation.mutate(address.id, {
      onSuccess: () => toast.success(t("addressDefaultSet")),
      onError: (err) => toast.error(getErrorMessage(err)),
    });
  }

  function handleDelete() {
    deleteMutation.mutate(address.id, {
      onSuccess: () => toast.success(t("addressDeleted")),
      onError: (err) => toast.error(getErrorMessage(err)),
    });
  }

  return (
    <div
      className={`rounded-3xl bg-white p-6 relative ${
        address.isDefault ? "border-2 border-mag/30" : "border border-ink/8"
      }`}
    >
      <span
        className={`absolute top-5 right-5 px-2 py-0.5 rounded-full text-[10px] font-bold ${
          address.isDefault ? "bg-mag text-white" : "bg-ink/8 text-ink/60"
        }`}
      >
        {address.isDefault ? t("defaultBadge") : t(ADDRESS_LABEL_KEYS[address.label])}
      </span>

      <div
        className={`w-10 h-10 rounded-2xl flex items-center justify-center mb-3 ${
          address.isDefault ? "bg-mag/10 text-mag" : "bg-ink/5 text-ink/60"
        }`}
      >
        <Icon className="h-5 w-5" />
      </div>

      <div className="font-black text-[15px]">{t(ADDRESS_LABEL_KEYS[address.label])}</div>
      <div className="text-ink/60 text-[13px] mt-1 leading-relaxed">
        {address.recipientName}
        <br />
        {address.line1}
        <br />
        {address.city}
        {address.postalCode ? ` ${address.postalCode}` : ""} · {address.country}
        <br />
        {address.phone}
      </div>

      <div className="mt-4 flex gap-2 text-[12px] font-bold">
        <button
          type="button"
          className="h-8 px-3 rounded-full border-2 border-ink/15"
          onClick={() => onEdit(address)}
        >
          {t("edit")}
        </button>
        {!address.isDefault && (
          <button
            type="button"
            className="h-8 px-3 rounded-full border-2 border-ink/15"
            disabled={setDefaultMutation.isPending}
            onClick={handleSetDefault}
          >
            {t("setDefault")}
          </button>
        )}
        <button
          type="button"
          className="h-8 px-3 rounded-full border-2 border-ink/15 text-sale"
          onClick={() => setConfirmDelete(true)}
        >
          {t("deleteAddress")}
        </button>
      </div>

      <AlertDialog open={confirmDelete} onOpenChange={setConfirmDelete}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{t("deleteAddress")}</AlertDialogTitle>
            <AlertDialogDescription>
              {t("confirmDeleteAddress")} — {t(ADDRESS_LABEL_KEYS[address.label])}, {address.line1}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>{t("cancel")}</AlertDialogCancel>
            <AlertDialogAction onClick={handleDelete} disabled={deleteMutation.isPending}>
              {t("deleteAddress")}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
