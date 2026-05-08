export interface Pickpoint {
  id: number;
  name: string;
  address: string;
  active: boolean;
}

export interface CreatePickpointPayload {
  name: string;
  address: string;
}

export interface UpdatePickpointPayload {
  name: string;
  address: string;
  active: boolean;
}
