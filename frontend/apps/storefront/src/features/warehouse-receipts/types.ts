export interface StockReceiptItemDto {
  itemId: number;
  skuId: number;
  skuCode: string;
  productName: string;
  quantityReceived: number;
  notes: string | null;
}

export interface StockReceiptDto {
  id: number;
  createdByUserId: number;
  createdAt: string;
  supplierReference: string | null;
  notes: string | null;
  status: string;
  items: StockReceiptItemDto[];
  totalUnitsReceived: number;
}

export interface LineItem {
  skuId: number;
  skuCode: string;
  productName: string;
  sizeLabel: string;
  quantity: number;
  notes: string;
}

export interface CreateStockReceiptRequest {
  supplierReference: string;
  notes: string;
  items: { skuId: number; quantity: number; notes: string }[];
}
