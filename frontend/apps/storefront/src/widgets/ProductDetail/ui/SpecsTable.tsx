"use client";

import { toast } from "sonner";
import { useTranslations } from "next-intl";
import type { ListingVariantDetail } from "@/entities/product";

interface SpecsTableProps {
  listing: ListingVariantDetail;
}

interface SpecRow {
  label: string;
  value: string;
  mono?: boolean;
  copyable?: boolean;
}

/**
 * `bg-soft` specs grid — article code (copy-on-click), brand, attributes,
 * dimensions, and weight. The warehouse SKU row is intentionally omitted.
 * Only rows with real values render.
 */
export default function SpecsTable({ listing }: SpecsTableProps) {
  const t = useTranslations("productDetail");

  const rows: SpecRow[] = [];

  if (listing.productCode) {
    rows.push({
      label: t("specArticle"),
      value: listing.productCode,
      mono: true,
      copyable: true,
    });
  }

  if (listing.brandName) {
    rows.push({ label: t("specBrand"), value: listing.brandName });
  }

  for (const attr of listing.attributes) {
    rows.push({ label: attr.key, value: attr.values.join(", ") });
  }

  if (listing.widthCm != null && listing.heightCm != null && listing.depthCm != null) {
    rows.push({
      label: t("specDimensions"),
      value: `${listing.widthCm} × ${listing.heightCm} × ${listing.depthCm} cm`,
    });
  }

  if (listing.weightKg != null) {
    rows.push({ label: t("specWeight"), value: `${listing.weightKg} kg` });
  }

  if (rows.length === 0) return null;

  function handleCopy(value: string) {
    navigator.clipboard.writeText(value).then(() => {
      toast.success(t("articleCopied"));
    });
  }

  return (
    <div className="bg-soft rounded-2xl md:rounded-3xl p-5 md:p-6">
      <h3 className="font-black text-xl">{t("specifications")}</h3>
      <div className="mt-4 grid grid-cols-2 gap-x-4 md:gap-x-8 gap-y-2 text-[12px] md:text-[13px]">
        {rows.map((row) => (
          <div key={row.label} className="flex justify-between border-b border-ink/8 pb-1.5">
            <span className="text-ink/55">{row.label}</span>
            {row.copyable ? (
              <button
                type="button"
                onClick={() => handleCopy(row.value)}
                className="font-mono text-[11px] cursor-pointer hover:text-mag transition-colors"
                title={t("clickToCopy")}
                aria-label={t("copyArticle")}
              >
                {row.value}
              </button>
            ) : (
              <span className={row.mono ? "font-mono text-[11px]" : "font-semibold"}>
                {row.value}
              </span>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
