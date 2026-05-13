export interface Pickpoint {
  id: number;
  name: string;
  address: string;
  active: boolean;
  latitude: number | null;
  longitude: number | null;
  hasParking: boolean;
  hasFittingRoom: boolean;
  hasCardPayment: boolean;
  wheelchairAccessible: boolean;
  timezone: string | null;
  temporarilyClosed: boolean;
  isOpenNow: boolean;
}

export interface CreatePickpointPayload {
  name: string;
  address: string;
  latitude: number | null;
  longitude: number | null;
  hasParking: boolean;
  hasFittingRoom: boolean;
  hasCardPayment: boolean;
  wheelchairAccessible: boolean;
}

export interface UpdatePickpointPayload {
  name: string;
  address: string;
  active: boolean;
  latitude: number | null;
  longitude: number | null;
  hasParking: boolean;
  hasFittingRoom: boolean;
  hasCardPayment: boolean;
  wheelchairAccessible: boolean;
  timezone: string | null;
  temporarilyClosed: boolean;
}

export interface PickpointHours {
  id: number;
  dayOfWeek: number; // 1=Mon … 7=Sun
  openTime: string;  // "HH:mm"
  closeTime: string; // "HH:mm"
}

export interface UpsertPickpointHoursPayload {
  dayOfWeek: number;
  openTime: string;
  closeTime: string;
}
