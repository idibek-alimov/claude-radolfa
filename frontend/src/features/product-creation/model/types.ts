export interface WizardAttribute {
  key: string;
  value: string;
  sortOrder: number;
}

export interface SkuRow {
  _key: string; // uuid for list diffing
  sizeLabel: string;
  price: number;
  stockQuantity: number;
  barcode: string;
  weightKg?: number;
  widthCm?: number;
  heightCm?: number;
  depthCm?: number;
}

export interface VariantDraft {
  colorId: number;
  isPublished: boolean; // default false — manager sign-off: variant is complete and intentional
  isActive: boolean;    // default true  — ops runtime toggle: show/hide without removing
  images: string[];
  skus: SkuRow[];
}

export interface WizardState {
  // Step 1
  name: string;
  categoryId: number | null;
  brandId: number | null;
  webDescription: string;
  attributes: WizardAttribute[];

  // Step 2 (one entry per color, owns images + SKUs)
  variants: VariantDraft[];
}

export const WIZARD_DRAFT_KEY = "radolfa:product-creation-draft";

export const INITIAL_WIZARD_STATE: WizardState = {
  name: "",
  categoryId: null,
  brandId: null,
  webDescription: "",
  attributes: [],
  variants: [],
};

export interface BlueprintEntryDto {
  key: string;
  required: boolean;
  suggestedValues?: string[];
}

export interface Step1Errors {
  name?: string;
  categoryId?: string;
}

export function validateStep1(state: WizardState): Step1Errors {
  const errors: Step1Errors = {};
  if (!state.name.trim()) errors.name = "Product name is required";
  if (state.categoryId === null) errors.categoryId = "Category is required";
  return errors;
}

export interface Step2Errors {
  emptySize: Set<string>;    // _key values with blank sizeLabel
  emptyBarcode: Set<string>; // _key values with blank barcode
}

export function validateStep2(state: WizardState): Step2Errors {
  const emptySize = new Set<string>();
  const emptyBarcode = new Set<string>();
  for (const variant of state.variants) {
    for (const row of variant.skus) {
      if (!row.sizeLabel.trim()) emptySize.add(row._key);
      if (!row.barcode.trim()) emptyBarcode.add(row._key);
    }
  }
  return { emptySize, emptyBarcode };
}

export function isStep2Valid(errors: Step2Errors): boolean {
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
