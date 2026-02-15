export interface UserDto {
  id: number;
  phone: string;
  role: "USER" | "MANAGER" | "SYSTEM";
  name?: string;
  email?: string;
  loyaltyPoints: number;
  enabled: boolean;
}

export interface PageResult<T> {
  items: T[];
  totalElements: number;
  page: number;
  hasMore: boolean;
}

export interface ToggleStatusParams {
  userId: number;
  enabled: boolean;
}

export interface ChangeRoleParams {
  userId: number;
  role: "USER" | "MANAGER";
}
