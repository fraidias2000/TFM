/**
 * Conexión WebSocket (STOMP) para recibir mensajes en tiempo real (RF-08, RNF-05).
 * El token se envía en el mensaje CONNECT. Si la conexión se pierde, se reintenta
 * cada 5 segundos; los mensajes perdidos se recuperan después con el historial (RF-14).
 */
import { Client } from "@stomp/stompjs"
import { API_URL } from "./api"
import type { MensajeApi } from "./mensajes"

/** Destino en el que el servidor envía los mensajes de los chats del usuario. */
const SUSCRIPCION_MENSAJES = "/user/queue/mensajes"

/** URL del WebSocket: VITE_WS_URL o, si no existe, la del API con /ws (http→ws). */
function urlWebSocket(): string {
  if (import.meta.env.VITE_WS_URL) return import.meta.env.VITE_WS_URL
  const url = new URL(API_URL, window.location.href)
  url.protocol = url.protocol === "https:" ? "wss:" : "ws:"
  url.pathname = "/ws"
  url.search = ""
  return url.toString()
}

/**
 * Abre la conexión y devuelve una función para cerrarla.
 * @param alRecibir se llama con cada mensaje recibido
 * @param alCambiarEstado se llama con true al conectar y con false al perder la conexión
 */
export function conectarTiempoReal(
  token: string,
  alRecibir: (mensaje: MensajeApi) => void,
  alCambiarEstado: (conectado: boolean) => void,
): () => void {
  const cliente = new Client({
    brokerURL: urlWebSocket(),
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 5_000,
    onConnect: () => {
      cliente.subscribe(SUSCRIPCION_MENSAJES, (frame) => {
        try {
          alRecibir(JSON.parse(frame.body) as MensajeApi)
        } catch {
          // Un mensaje mal formado se ignora; se recuperará con el historial.
        }
      })
      alCambiarEstado(true)
    },
    onWebSocketClose: () => alCambiarEstado(false),
    onStompError: () => alCambiarEstado(false),
  })
  cliente.activate()
  return () => {
    void cliente.deactivate()
  }
}
