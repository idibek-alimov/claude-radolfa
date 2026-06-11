"use client";

import { useEffect, useState, type KeyboardEvent } from "react";
import { cn } from "@radolfa/shared/lib/utils";
import type { CatalogCriteria, CatalogFacets } from "@/entities/product";

interface FilterPanelProps {
  facets?: CatalogFacets;
  value: CatalogCriteria;
  onChange: (patch: Partial<CatalogCriteria>) => void;
  onReset: () => void;
  /** Selected category slug (independent of `value` — owned by the route). */
  selectedCategorySlug?: string | null;
  onCategorySelect?: (slug: string | null) => void;
  /** Show the "Apply filters" button (mobile/staged mode). */
  showApply?: boolean;
  onApply?: () => void;
}

function toggleInArray<T>(arr: T[] | undefined, item: T): T[] | undefined {
  const next = arr?.includes(item)
    ? arr.filter((v) => v !== item)
    : [...(arr ?? []), item];
  return next.length ? next : undefined;
}

export function FilterPanel({
  facets,
  value,
  onChange,
  onReset,
  selectedCategorySlug,
  onCategorySelect,
  showApply,
  onApply,
}: FilterPanelProps) {
  const [priceMinInput, setPriceMinInput] = useState(value.priceMin?.toString() ?? "");
  const [priceMaxInput, setPriceMaxInput] = useState(value.priceMax?.toString() ?? "");

  useEffect(() => {
    setPriceMinInput(value.priceMin?.toString() ?? "");
  }, [value.priceMin]);

  useEffect(() => {
    setPriceMaxInput(value.priceMax?.toString() ?? "");
  }, [value.priceMax]);

  const commitPriceMin = () => {
    const n = priceMinInput === "" ? undefined : Number(priceMinInput);
    onChange({ priceMin: n != null && Number.isFinite(n) ? n : undefined });
  };

  const commitPriceMax = () => {
    const n = priceMaxInput === "" ? undefined : Number(priceMaxInput);
    onChange({ priceMax: n != null && Number.isFinite(n) ? n : undefined });
  };

  const handlePriceKeyDown = (
    e: KeyboardEvent<HTMLInputElement>,
    commit: () => void
  ) => {
    if (e.key === "Enter") {
      e.currentTarget.blur();
      commit();
    }
  };

  const priceBounds = facets?.price;

  const discountOptions: { label: string; min: number }[] = [
    { label: "Any discount", min: 1 },
    { label: "30% and more", min: 30 },
    { label: "50% and more", min: 50 },
  ];

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <div className="font-black text-[15px]">Filters</div>
        <button
          onClick={onReset}
          className="text-[12px] text-mag font-bold hover:underline"
        >
          Reset
        </button>
      </div>

      {!!facets?.categories.length && (
        <div>
          <div className="font-bold text-[13px] mb-2">Category</div>
          <ul className="space-y-1.5 text-ink/75">
            {facets.categories.map((category) => (
              <li key={category.id}>
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    className="accent-mag w-4 h-4"
                    checked={selectedCategorySlug === category.slug}
                    onChange={() =>
                      onCategorySelect?.(
                        selectedCategorySlug === category.slug ? null : category.slug
                      )
                    }
                  />
                  {category.name}
                </label>
              </li>
            ))}
          </ul>
        </div>
      )}

      <hr className="border-ink/10" />

      <div>
        <div className="font-bold text-[13px] mb-2">Price, TJS</div>
        <div className="flex items-center gap-2">
          <input
            className="w-full h-9 px-2.5 rounded-lg bg-white border border-ink/15 text-[12px]"
            placeholder={priceBounds ? `from ${priceBounds.min}` : "from"}
            inputMode="numeric"
            value={priceMinInput}
            onChange={(e) => setPriceMinInput(e.target.value)}
            onBlur={commitPriceMin}
            onKeyDown={(e) => handlePriceKeyDown(e, commitPriceMin)}
          />
          <span className="text-ink/40">–</span>
          <input
            className="w-full h-9 px-2.5 rounded-lg bg-white border border-ink/15 text-[12px]"
            placeholder={priceBounds ? `to ${priceBounds.max}` : "to"}
            inputMode="numeric"
            value={priceMaxInput}
            onChange={(e) => setPriceMaxInput(e.target.value)}
            onBlur={commitPriceMax}
            onKeyDown={(e) => handlePriceKeyDown(e, commitPriceMax)}
          />
        </div>
      </div>

      <hr className="border-ink/10" />

      <div>
        <div className="font-bold text-[13px] mb-2">Discount</div>
        <ul className="space-y-1.5 text-ink/75">
          {discountOptions.map((option) => (
            <li key={option.label}>
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  className="accent-mag w-4 h-4"
                  checked={value.minDiscount === option.min}
                  onChange={() =>
                    onChange({
                      minDiscount: value.minDiscount === option.min ? undefined : option.min,
                    })
                  }
                />
                {option.label}
              </label>
            </li>
          ))}
        </ul>
      </div>

      {!!facets?.brands.length && (
        <>
          <hr className="border-ink/10" />
          <div>
            <div className="font-bold text-[13px] mb-2">Brand</div>
            <ul className="space-y-1.5 text-ink/75">
              {facets.brands.map((brand) => (
                <li key={brand.id}>
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="checkbox"
                      className="accent-mag w-4 h-4"
                      checked={!!value.brandIds?.includes(brand.id)}
                      onChange={() =>
                        onChange({ brandIds: toggleInArray(value.brandIds, brand.id) })
                      }
                    />
                    {brand.name}
                  </label>
                </li>
              ))}
            </ul>
          </div>
        </>
      )}

      {!!facets?.colors.length && (
        <>
          <hr className="border-ink/10" />
          <div>
            <div className="font-bold text-[13px] mb-2">Colour</div>
            <div className="flex flex-wrap gap-2">
              {facets.colors.map((color) => {
                const selected = !!value.colorKeys?.includes(color.key);
                return (
                  <button
                    key={color.key}
                    title={color.name}
                    aria-label={color.name}
                    onClick={() =>
                      onChange({ colorKeys: toggleInArray(value.colorKeys, color.key) })
                    }
                    className={cn(
                      "w-7 h-7 rounded-lg",
                      selected ? "ring-2 ring-mag ring-offset-1" : "ring-1 ring-ink/15"
                    )}
                    style={{ background: color.hex }}
                  />
                );
              })}
            </div>
          </div>
        </>
      )}

      <hr className="border-ink/10" />

      <div className="space-y-2 text-ink/75">
        <label className="flex items-center gap-2 cursor-pointer">
          <input
            type="checkbox"
            className="accent-mag w-4 h-4"
            checked={!!value.inStock}
            onChange={(e) => onChange({ inStock: e.target.checked || undefined })}
          />
          In stock
        </label>
      </div>

      {showApply && (
        <button
          onClick={onApply}
          className="w-full h-10 rounded-full bg-mag text-white font-bold text-[13px] hover:bg-maglo"
        >
          Apply filters
        </button>
      )}
    </div>
  );
}
