import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

/**
 * Creates a new STOMP client connected to the backend `/ws` endpoint via SockJS.
 *
 * Each call returns a fresh client — callers (e.g. useDeliverySocket) own their instance's
 * lifecycle and must call client.deactivate() on cleanup.
 *
 * Cookies flow automatically with SockJS XHR transports on same-origin requests —
 * no explicit token injection is required for browser clients.
 */
export function createStompClient(): Client {
  const baseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "";

  return new Client({
    webSocketFactory: () => new SockJS(`${baseUrl}/ws`),
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
  });
}
