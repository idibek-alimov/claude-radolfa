export interface PickQueueItem {
  orderId: number;
  externalOrderId: string;
  deliveryType: "HOME" | "PICKPOINT";
  createdAt: string;
  customerName: string;
  totalUnits: number;
  pickedUnits: number;
}
