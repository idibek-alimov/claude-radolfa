export interface WizardAttribute {
  key: string;
  value: string;
  sortOrder: number;
}

export interface SkuRow {
  _key: string; // uuid for list diffing
  colorId: number;
  sizeLabel: string;
  price: number;
  stockQuantity: number;
  barcode: string;
  weightKg?: number;
  widthCm?: number;
  heightCm?: number;
  depthCm?: number;
}

export interface WizardState {
  // Step 1
  name: string;
  categoryId: number | null;
  brandId: number | null;
  colorIds: number[];
  webDescription: string;
  attributes: WizardAttribute[];

  // Step 2 (per-color images)
  imagesByColorId: Record<number, string[]>;

  // Step 3 (per-color+size SKU table)
  skuRows: SkuRow[];
}

export const WIZARD_DRAFT_KEY = "radolfa:product-creation-draft";

export const INITIAL_WIZARD_STATE: WizardState = {
  name: "",
  categoryId: null,
  brandId: null,
  colorIds: [],
  webDescription: "",
  attributes: [],
  imagesByColorId: {},
  skuRows: [],
};

export interface BlueprintEntryDto {
  key: string;
  required: boolean;
  suggestedValues?: string[];
}

export interface Step1Errors {
  name?: string;
  categoryId?: string;
  colorIds?: string;
}

export function validateStep1(state: WizardState): Step1Errors {
  const errors: Step1Errors = {};
  if (!state.name.trim()) errors.name = "Product name is required";
  if (state.categoryId === null) errors.categoryId = "Category is required";
  if (state.colorIds.length === 0) errors.colorIds = "Select at least one color";
  return errors;
}

export interface Step3Errors {
  emptySize: Set<string>;   // _key values with blank sizeLabel
  emptyBarcode: Set<string>; // _key values with blank barcode
}

export function validateStep3(state: WizardState): Step3Errors {
  const emptySize = new Set<string>();
  const emptyBarcode = new Set<string>();
  for (const row of state.skuRows) {
    if (!row.sizeLabel.trim()) emptySize.add(row._key);
    if (!row.barcode.trim()) emptyBarcode.add(row._key);
  }
  return { emptySize, emptyBarcode };
}

export function isStep3Valid(errors: Step3Errors): boolean {
  return errors.emptySize.size === 0 && errors.emptyBarcode.size === 0;
}

// Returns set of attribute keys that are required by the blueprint but have empty values
export function validateStep4(
  state: WizardState,
  blueprint: BlueprintEntryDto[]
): Set<string> {
  const failing = new Set<string>();
  for (const entry of blueprint) {
    if (!entry.required) continue;
    const attr = state.attributes.find((a) => a.key === entry.key);
    if (!attr || !attr.value.trim()) failing.add(entry.key);
  }
  return failing;
}
