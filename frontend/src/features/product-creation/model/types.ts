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
