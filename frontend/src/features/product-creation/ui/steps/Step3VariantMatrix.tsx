"use client";

import { useEffect, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Plus, Trash2, ChevronDown } from "lucide-react";
import { fetchColors } from "@/entities/color";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { cn } from "@/shared/lib/utils";
import type { WizardState, SkuRow, Step3Errors } from "../../model/types";

interface Props {
  state: WizardState;
  update: (patch: Partial<WizardState>) => void;
  submitted: boolean;
  errors: Step3Errors;
}

function makeRow(colorId: number, sizeLabel = ""): SkuRow {
  return {
    _key: crypto.randomUUID(),
    colorId,
    sizeLabel,
    price: 0,
    stockQuantity: 0,
    barcode: "",
  };
}

export function Step3VariantMatrix({ state, update, submitted, errors }: Props) {
  const { data: colors } = useQuery({
    queryKey: ["colors"],
    queryFn: fetchColors,
  });

  const seeded = useRef(false);
  const [showLogistics, setShowLogistics] = useState(false);

  // Auto-seed one "ONE_SIZE" row per color that has no rows yet
  useEffect(() => {
    if (seeded.current || state.colorIds.length === 0) return;
    seeded.current = true;

    const missing = state.colorIds.filter(
      (cid) => !state.skuRows.some((r) => r.colorId === cid)
    );
    if (missing.length === 0) return;

    const newRows: SkuRow[] = missing.map((cid) => makeRow(cid, "ONE_SIZE"));
    update({ skuRows: [...state.skuRows, ...newRows] });
  }, [state.colorIds, state.skuRows, update]);

  const colorMap = Object.fromEntries(
    (colors ?? []).map((c) => [c.id, c])
  );

  function updateRow(key: string, patch: Partial<SkuRow>) {
    update({
      skuRows: state.skuRows.map((r) =>
        r._key === key ? { ...r, ...patch } : r
      ),
    });
  }

  function addRow(colorId: number) {
    update({ skuRows: [...state.skuRows, makeRow(colorId)] });
  }

  function deleteRow(key: string) {
    update({ skuRows: state.skuRows.filter((r) => r._key !== key) });
  }

  function applyToColor(colorId: number, field: "price" | "stockQuantity") {
    const colorRows = state.skuRows.filter((r) => r.colorId === colorId);
    if (colorRows.length < 2) return;
    const firstValue = colorRows[0][field];
    update({
      skuRows: state.skuRows.map((r) =>
        r.colorId === colorId ? { ...r, [field]: firstValue } : r
      ),
    });
  }

  // Render rows grouped by colorId order
  const groups = state.colorIds.map((cid) => ({
    colorId: cid,
    color: colorMap[cid],
    rows: state.skuRows.filter((r) => r.colorId === cid),
  }));

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h2 className="text-lg font-semibold">Variant Matrix</h2>
        <p className="text-sm text-muted-foreground mt-0.5">
          Define sizes, barcodes, prices, and stock for each color variant.
        </p>
      </div>

      {/* Logistics toggle */}
      <button
        type="button"
        onClick={() => setShowLogistics((v) => !v)}
        className="flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors"
      >
        <ChevronDown
          className={cn(
            "h-4 w-4 transition-transform",
            showLogistics && "rotate-180"
          )}
        />
        {showLogistics ? "Hide" : "Show"} logistics fields (weight, dimensions)
      </button>

      {/* Table */}
      <div className="overflow-x-auto rounded-lg border">
        <table className="w-full text-sm border-collapse">
          <thead>
            <tr className="border-b bg-muted/50">
              <th className="px-3 py-2.5 text-left font-medium text-muted-foreground whitespace-nowrap">
                Color
              </th>
              <th className="px-3 py-2.5 text-left font-medium text-muted-foreground whitespace-nowrap">
                Size Label <span className="text-destructive">*</span>
              </th>
              <th className="px-3 py-2.5 text-left font-medium text-muted-foreground whitespace-nowrap">
                Barcode <span className="text-destructive">*</span>
              </th>
              <th className="px-3 py-2.5 text-left font-medium text-muted-foreground whitespace-nowrap min-w-[120px]">
                <span>Price (TJS)</span>
              </th>
              <th className="px-3 py-2.5 text-left font-medium text-muted-foreground whitespace-nowrap min-w-[100px]">
                <span>Stock</span>
              </th>
              {showLogistics && (
                <>
                  <th className="px-3 py-2.5 text-left font-medium text-muted-foreground whitespace-nowrap">
                    Weight (kg)
                  </th>
                  <th className="px-3 py-2.5 text-left font-medium text-muted-foreground whitespace-nowrap">
                    W (cm)
                  </th>
                  <th className="px-3 py-2.5 text-left font-medium text-muted-foreground whitespace-nowrap">
                    H (cm)
                  </th>
                  <th className="px-3 py-2.5 text-left font-medium text-muted-foreground whitespace-nowrap">
                    D (cm)
                  </th>
                </>
              )}
              <th className="px-3 py-2.5 w-10" />
            </tr>
          </thead>
          <tbody>
            {groups.map((group, gi) => (
              <>
                {/* Apply-to-color bar for this group */}
                {group.rows.length > 1 && (
                  <tr
                    key={`apply-${group.colorId}`}
                    className={cn(
                      "border-b",
                      gi % 2 === 0 ? "bg-muted/20" : "bg-background"
                    )}
                  >
                    <td
                      colSpan={showLogistics ? 10 : 6}
                      className="px-3 py-1"
                    >
                      <div className="flex items-center gap-3 text-xs text-muted-foreground">
                        <span>Apply first row value to all rows in this color:</span>
                        <button
                          type="button"
                          onClick={() => applyToColor(group.colorId, "price")}
                          className="underline underline-offset-2 hover:text-foreground"
                        >
                          ⬇ Price
                        </button>
                        <button
                          type="button"
                          onClick={() =>
                            applyToColor(group.colorId, "stockQuantity")
                          }
                          className="underline underline-offset-2 hover:text-foreground"
                        >
                          ⬇ Stock
                        </button>
                      </div>
                    </td>
                  </tr>
                )}

                {group.rows.map((row) => {
                  const sizeErr =
                    submitted && errors.emptySize.has(row._key);
                  const barcodeErr =
                    submitted && errors.emptyBarcode.has(row._key);

                  return (
                    <tr
                      key={row._key}
                      className={cn(
                        "border-b last:border-b-0 hover:bg-muted/30 transition-colors",
                        gi % 2 === 0 ? "bg-muted/10" : "bg-background"
                      )}
                    >
                      {/* Color chip */}
                      <td className="px-3 py-2">
                        <div className="flex items-center gap-1.5 whitespace-nowrap">
                          <span
                            className="h-3 w-3 rounded-full border border-black/10 shrink-0"
                            style={{
                              backgroundColor:
                                group.color?.hexCode ?? "#e5e7eb",
                            }}
                          />
                          <span className="text-xs text-muted-foreground">
                            {group.color?.displayName ??
                              group.color?.colorKey ??
                              `#${group.colorId}`}
                          </span>
                        </div>
                      </td>

                      {/* Size label */}
                      <td className="px-3 py-2">
                        <Input
                          value={row.sizeLabel}
                          onChange={(e) =>
                            updateRow(row._key, { sizeLabel: e.target.value })
                          }
                          placeholder="S, M, L, 42, ONE_SIZE…"
                          className={cn(
                            "h-8 w-32",
                            sizeErr && "border-destructive focus-visible:ring-destructive"
                          )}
                        />
                        {sizeErr && (
                          <p className="text-xs text-destructive mt-0.5">
                            Required
                          </p>
                        )}
                      </td>

                      {/* Barcode */}
                      <td className="px-3 py-2">
                        <Input
                          value={row.barcode}
                          onChange={(e) =>
                            updateRow(row._key, { barcode: e.target.value })
                          }
                          placeholder="EAN / UPC"
                          className={cn(
                            "h-8 w-36",
                            barcodeErr &&
                              "border-destructive focus-visible:ring-destructive"
                          )}
                        />
                        {barcodeErr && (
                          <p className="text-xs text-destructive mt-0.5">
                            Required
                          </p>
                        )}
                      </td>

                      {/* Price */}
                      <td className="px-3 py-2">
                        <Input
                          type="number"
                          min={0}
                          step={0.01}
                          value={row.price}
                          onChange={(e) =>
                            updateRow(row._key, {
                              price: parseFloat(e.target.value) || 0,
                            })
                          }
                          className="h-8 w-28"
                        />
                      </td>

                      {/* Stock */}
                      <td className="px-3 py-2">
                        <Input
                          type="number"
                          min={0}
                          step={1}
                          value={row.stockQuantity}
                          onChange={(e) =>
                            updateRow(row._key, {
                              stockQuantity: parseInt(e.target.value) || 0,
                            })
                          }
                          className="h-8 w-24"
                        />
                      </td>

                      {/* Logistics (optional) */}
                      {showLogistics && (
                        <>
                          <td className="px-3 py-2">
                            <Input
                              type="number"
                              min={0}
                              step={0.01}
                              placeholder="—"
                              value={row.weightKg ?? ""}
                              onChange={(e) =>
                                updateRow(row._key, {
                                  weightKg: e.target.value
                                    ? parseFloat(e.target.value)
                                    : undefined,
                                })
                              }
                              className="h-8 w-20"
                            />
                          </td>
                          <td className="px-3 py-2">
                            <Input
                              type="number"
                              min={0}
                              placeholder="—"
                              value={row.widthCm ?? ""}
                              onChange={(e) =>
                                updateRow(row._key, {
                                  widthCm: e.target.value
                                    ? parseFloat(e.target.value)
                                    : undefined,
                                })
                              }
                              className="h-8 w-20"
                            />
                          </td>
                          <td className="px-3 py-2">
                            <Input
                              type="number"
                              min={0}
                              placeholder="—"
                              value={row.heightCm ?? ""}
                              onChange={(e) =>
                                updateRow(row._key, {
                                  heightCm: e.target.value
                                    ? parseFloat(e.target.value)
                                    : undefined,
                                })
                              }
                              className="h-8 w-20"
                            />
                          </td>
                          <td className="px-3 py-2">
                            <Input
                              type="number"
                              min={0}
                              placeholder="—"
                              value={row.depthCm ?? ""}
                              onChange={(e) =>
                                updateRow(row._key, {
                                  depthCm: e.target.value
                                    ? parseFloat(e.target.value)
                                    : undefined,
                                })
                              }
                              className="h-8 w-20"
                            />
                          </td>
                        </>
                      )}

                      {/* Delete */}
                      <td className="px-3 py-2">
                        <button
                          type="button"
                          onClick={() => deleteRow(row._key)}
                          disabled={group.rows.length === 1}
                          className="h-8 w-8 flex items-center justify-center rounded-md text-muted-foreground hover:text-destructive hover:bg-destructive/10 transition-colors disabled:opacity-30 disabled:pointer-events-none"
                          aria-label="Delete row"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </td>
                    </tr>
                  );
                })}

                {/* Add size row */}
                <tr
                  key={`add-${group.colorId}`}
                  className={cn(
                    "border-b",
                    gi % 2 === 0 ? "bg-muted/10" : "bg-background"
                  )}
                >
                  <td
                    colSpan={showLogistics ? 10 : 6}
                    className="px-3 py-2"
                  >
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={() => addRow(group.colorId)}
                      className="h-7 gap-1.5 text-xs text-muted-foreground hover:text-foreground"
                    >
                      <Plus className="h-3.5 w-3.5" />
                      Add size for{" "}
                      {group.color?.displayName ??
                        group.color?.colorKey ??
                        `color #${group.colorId}`}
                    </Button>
                  </td>
                </tr>
              </>
            ))}
          </tbody>
        </table>
      </div>

      {/* Summary */}
      <p className="text-xs text-muted-foreground">
        {state.skuRows.length} SKU{state.skuRows.length !== 1 ? "s" : ""} across{" "}
        {state.colorIds.length} color{state.colorIds.length !== 1 ? "s" : ""}
      </p>
    </div>
  );
}
