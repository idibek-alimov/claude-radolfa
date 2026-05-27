export interface PickQueueItem {
  orderId: number;
  externalOrderId: string;
  deliveryType: "HOME" | "PICKPOINT";
  createdAt: string;
  customerName: string;
  totalUnits: number;
  pickedUnits: number;
}

export interface PickPlacement {
  binLabel: string | null;
  quantity: number;
}

export interface PickSessionItem {
  orderItemId: number;
  skuCode: string;
  barcode: string;
  productName: string;
  sizeLabel: string;
  quantity: number;
  quantityPicked: number;
  placements: PickPlacement[];
}

export interface PickSession {
  orderId: number;
  externalOrderId: string;
  deliveryType: "HOME" | "PICKPOINT";
  status: string;
  items: PickSessionItem[];
}

export interface ScanResult {
  orderItemId: number;
  quantityPicked: number;
  quantityOrdered: number;
  orderFullyPicked: boolean;
}
