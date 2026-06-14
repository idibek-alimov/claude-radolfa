"use client";

import { useState } from "react";
import { Plus, Sparkles } from "lucide-react";
import { useTranslations } from "next-intl";
import { Skeleton } from "@radolfa/shared/ui/skeleton";
import { useMyAddresses, type Address } from "@/entities/address";
import { AddressCard } from "./AddressCard";
import { AddAddressTile } from "./AddAddressTile";
import { AddressFormDialog } from "./AddressFormDialog";

/** Dashed empty state — design-system "coming soon" / no-data block. */
function EmptyBlock({ message }: { message: string }) {
  return (
    <div className="border border-dashed border-ink/15 rounded-xl p-12 flex flex-col items-center text-center gap-3">
      <Sparkles className="h-10 w-10 text-ink/20" />
      <p className="text-sm text-ink/55">{message}</p>
    </div>
  );
}

// Phase 10 — real address-book CRUD on top of entities/address.
export function AddressBookPage() {
  const t = useTranslations("profile");
  const { data: addresses, isLoading } = useMyAddresses();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingAddress, setEditingAddress] = useState<Address | null>(null);

  function openCreate() {
    setEditingAddress(null);
    setDialogOpen(true);
  }

  function openEdit(address: Address) {
    setEditingAddress(address);
    setDialogOpen(true);
  }

  return (
    <section className="space-y-5">
      <div className="flex items-end justify-between flex-wrap gap-3">
        <div>
          <h1 className="text-[28px] font-black leading-tight">{t("sectionAddresses")}</h1>
          <p className="text-ink/55 text-[13px] mt-1">{t("addressSubtitle")}</p>
        </div>
        <button
          type="button"
          onClick={openCreate}
          className="h-10 px-5 rounded-full bg-mag text-white font-bold text-[13px] inline-flex items-center gap-2 hover:bg-maglo"
        >
          <Plus className="h-4 w-4" />
          {t("addAddress")}
        </button>
      </div>

      {isLoading ? (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
          <Skeleton className="h-[180px] w-full rounded-3xl" />
          <Skeleton className="h-[180px] w-full rounded-3xl" />
        </div>
      ) : !addresses || addresses.length === 0 ? (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
          <div className="lg:col-span-2">
            <EmptyBlock message={t("addressEmpty")} />
          </div>
          <AddAddressTile onClick={openCreate} />
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
          {addresses.map((address) => (
            <AddressCard key={address.id} address={address} onEdit={openEdit} />
          ))}
          <AddAddressTile onClick={openCreate} />
        </div>
      )}

      <AddressFormDialog open={dialogOpen} onOpenChange={setDialogOpen} address={editingAddress} />
    </section>
  );
}
