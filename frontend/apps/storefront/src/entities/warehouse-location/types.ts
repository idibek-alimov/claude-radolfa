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
