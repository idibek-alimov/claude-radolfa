// ── Public API of the address entity slice ──────────────────────
export type { Address, AddressLabel, AddressRequest } from "./model/types";
export {
  useMyAddresses,
  useCreateAddress,
  useUpdateAddress,
  useDeleteAddress,
  useSetDefaultAddress,
} from "./api";
