export type { SkuLookupResponse } from "@/entities/warehouse-sku";

export const INVENTORY_TRANSACTION_TYPES = [
  "SALE",
  "CANCELLATION",
  "RECALL_RETURN",
  "RETURN_RESTORE",
  "WRITE_OFF",
  "RECEIPT",
  "MANUAL_ADJUSTMENT",
] as const;

export type InventoryTransactionType = (typeof INVENTORY_TRANSACTION_TYPES)[number];

export interface InventoryTransactionRecord {
  id: number;
  skuId: number;
  delta: number;
  type: InventoryTransactionType;
  referenceType: string | null;
  referenceId: number | null;
  actorUserId: number | null;
  notes: string | null;
  occurredAt: string;
}

export interface WarehouseZoneDto {
  id: number;
  code: string;
  label: string;
}

export interface WarehouseShelfDto {
  id: number;
  zoneId: number;
  code: string;
  label: string;
}

export interface WarehouseBinDto {
  id: number;
  shelfId: number;
  code: string;
}
