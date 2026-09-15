import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { TOKEN_STORAGE_KEY } from "./apiClient";

const API_BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";

export function createQueueSocketClient({ onAdminQueue, onEsperaQueue, onPrepaidQueue, onError }) {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY);

  const client = new Client({
    webSocketFactory: () => new SockJS(`${API_BASE_URL}/ws`),
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    debug: (str) => {
      if (import.meta.env.DEV) {
        console.log("[STOMP Debug]:", str);
      }
    },
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
  });

  client.onConnect = () => {
    console.log("WebSocket STOMP conectado exitosamente a /ws");

    if (onAdminQueue) {
      client.subscribe("/topic/cola/admin", (message) => {
        try {
          const data = JSON.parse(message.body);
          onAdminQueue(data);
        } catch (e) {
          console.error("Error al procesar mensaje en /topic/cola/admin", e);
        }
      });
    }

    if (onEsperaQueue) {
      client.subscribe("/topic/cola/espera", (message) => {
        try {
          const data = JSON.parse(message.body);
          onEsperaQueue(data);
        } catch (e) {
          console.error("Error al procesar mensaje en /topic/cola/espera", e);
        }
      });
    }

    if (onPrepaidQueue) {
      client.subscribe("/topic/cola/prepagada", (message) => {
        try {
          const data = JSON.parse(message.body);
          onPrepaidQueue(data);
        } catch (e) {
          console.error("Error al procesar mensaje en /topic/cola/prepagada", e);
        }
      });
    }
  };

  client.onStompError = (frame) => {
    console.error("STOMP Error en WebSocket:", frame.headers["message"], frame.body);
    if (onError) onError(frame);
  };

  return client;
}
