export interface SkuLookupResponse {
  skuId: number;
  skuCode: string;
  barcode: string;
  productName: string;
  sizeLabel: string;
  stockQuantity: number;
  binLocation: string | null;
}
