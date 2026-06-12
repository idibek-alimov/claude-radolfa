// Synced from backend — do not edit manually
export type DeliveryType = "HOME" | "PICKPOINT";
export type TimeWindowCode = "MORNING" | "AFTERNOON" | "EVENING";

export const TIME_WINDOW_CODES: ReadonlyArray<TimeWindowCode> = [
  "MORNING",
  "AFTERNOON",
  "EVENING",
] as const;
