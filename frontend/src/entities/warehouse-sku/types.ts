export interface Placement {
  binId: number | null;
  binLabel: string | null;
  quantity: number;
}

export interface SkuLookupResponse {
  skuId: number;
  skuCode: string;
  barcode: string;
  productName: string;
  sizeLabel: string;
  stockQuantity: number;
  placements: Placement[];
}
