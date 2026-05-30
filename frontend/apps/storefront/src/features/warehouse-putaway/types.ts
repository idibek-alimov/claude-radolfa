export interface InboundQueueItem {
  skuId: number;
  skuCode: string;
  barcode: string;
  productName: string;
  unassignedQuantity: number;
}
