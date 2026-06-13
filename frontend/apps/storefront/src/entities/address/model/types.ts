// Synced from backend — do not edit manually
export type AddressLabel = "HOME" | "WORK" | "OTHER";

export interface Address {
  id: number;
  label: AddressLabel;
  recipientName: string;
  phone: string;
  line1: string;
  city: string;
  postalCode: string | null;
  country: string;
  isDefault: boolean;
}

/**
 * Request body for creating/updating an address. The owner id and `isDefault`
 * (on update) are never accepted here — ownership comes from the security
 * principal, and the default flag is managed exclusively via
 * `PATCH /addresses/{id}/default`.
 */
export interface AddressRequest {
  label: AddressLabel;
  recipientName: string;
  phone: string;
  line1: string;
  city: string;
  postalCode: string | null;
  country: string;
  isDefault: boolean;
}
