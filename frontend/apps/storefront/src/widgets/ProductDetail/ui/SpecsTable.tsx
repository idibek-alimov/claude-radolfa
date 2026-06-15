import { useTranslations } from "next-intl";
import type { ListingVariantDetail, Sku } from "@/entities/product";

interface SpecsTableProps {
  listing: ListingVariantDetail;
  selectedSku: Sku | null;
}

interface SpecRow {
  label: string;
  value: string;
  mono?: boolean;
}

/**
 * `bg-soft` specs grid — brand, attributes, dimensions, weight, and SKU.
 * Only rows with real values render; the monolith's first-5 "Show all"
 * expander is dropped per Phase 4 (the detail object is fully loaded).
 */
export default function SpecsTable({ listing, selectedSku }: SpecsTableProps) {
  const t = useTranslations("productDetail");

  const rows: SpecRow[] = [];

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

  const skuCode = selectedSku?.skuCode ?? listing.productCode;
  if (skuCode) {
    rows.push({ label: t("specSku"), value: skuCode, mono: true });
  }

  if (rows.length === 0) return null;

  return (
    <div className="bg-soft rounded-2xl md:rounded-3xl p-5 md:p-6">
      <h3 className="font-black text-xl">{t("specifications")}</h3>
      <div className="mt-4 grid grid-cols-2 gap-x-4 md:gap-x-8 gap-y-2 text-[12px] md:text-[13px]">
        {rows.map((row) => (
          <div key={row.label} className="flex justify-between border-b border-ink/8 pb-1.5">
            <span className="text-ink/55">{row.label}</span>
            <span className={row.mono ? "font-mono text-[11px]" : "font-semibold"}>
              {row.value}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}
