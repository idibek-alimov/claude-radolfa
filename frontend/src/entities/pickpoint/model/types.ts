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
}
