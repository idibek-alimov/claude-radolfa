// Synced from backend — do not edit manually

/** Request body for POST /api/v1/users/phone/change-request. */
export interface PhoneChangeRequest {
  phone: string;
}

/** Request body for POST /api/v1/users/phone/change-verify. */
export interface PhoneChangeVerify {
  phone: string;
  otp: string;
}
