"use client";

import { Plus } from "lucide-react";
import { useTranslations } from "next-intl";

interface AddAddressTileProps {
  onClick: () => void;
}

/** Dashed "add new address" tile — design-system add affordance. */
export function AddAddressTile({ onClick }: AddAddressTileProps) {
  const t = useTranslations("profile");

  return (
    <button
      type="button"
      onClick={onClick}
      className="rounded-3xl border-2 border-dashed border-ink/15 p-6 flex flex-col items-center justify-center text-ink/45 hover:border-mag/40 hover:text-mag transition min-h-[180px]"
    >
      <Plus className="h-7 w-7" />
      <span className="mt-2 font-bold text-[13px]">{t("addNewAddress")}</span>
    </button>
  );
}
