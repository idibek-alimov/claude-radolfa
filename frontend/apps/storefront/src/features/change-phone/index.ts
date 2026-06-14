// ── Public API of the change-phone feature slice ─────────────────
export type { PhoneChangeRequest, PhoneChangeVerify } from "./model/types";
export { useRequestPhoneChange, useVerifyPhoneChange } from "./api";
export { ChangePhoneDialog } from "./ui/ChangePhoneDialog";
