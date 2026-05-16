"use client";

import { useEffect, useRef, useState } from "react";
import { createStompClient } from "@/shared/api/websocket";

export type DeliveryEvent = {
  type: string;
  orderId: number;
  courierId?: number;
  pickpointId?: number;
};

type Options = {
  topic: string;
  onMessage: (event: DeliveryEvent) => void;
};

/**
 * Subscribes to a STOMP delivery-event topic and calls onMessage on each frame.
 *
 * Usage (Phases 10/11 dashboards):
 *   const { connected } = useDeliverySocket({
 *     topic: "/user/queue/delivery",
 *     onMessage: (e) => refetchOrders(),
 *   });
 */
export function useDeliverySocket({ topic, onMessage }: Options): { connected: boolean } {
  const [connected, setConnected] = useState(false);
  const onMessageRef = useRef(onMessage);
  onMessageRef.current = onMessage;

  useEffect(() => {
    const client = createStompClient();

    client.onConnect = () => {
      setConnected(true);
      client.subscribe(topic, (frame) => {
        try {
          const payload = JSON.parse(frame.body) as DeliveryEvent;
          onMessageRef.current(payload);
        } catch {
          // malformed frame — ignore
        }
      });
    };

    client.onWebSocketClose = () => setConnected(false);

    client.activate();

    return () => {
      client.deactivate();
    };
  }, [topic]); // re-subscribe only if topic changes

  return { connected };
}
