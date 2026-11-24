/**
 * Conexión WebSocket (STOMP) para recibir en tiempo real los mensajes (RF-08,
 * RNF-05) y los avisos de nuevos chats cercanos (RF-06). El token se envía en el
 * mensaje CONNECT. Si la conexión se pierde, se reintenta cada 5 segundos; los
 * mensajes perdidos se recuperan después con el historial (RF-14).
 */
import { Client } from "@stomp/stompjs"
import { API_URL } from "./api"
import type { AvisoApi } from "./avisos"
import type { MensajeApi } from "./mensajes"

/** Destinos propios del usuario en los que el servidor envía mensajes y avisos. */
const SUSCRIPCION_MENSAJES = "/user/queue/mensajes"
const SUSCRIPCION_AVISOS = "/user/queue/avisos"

/** URL del WebSocket: VITE_WS_URL o, si no existe, la del API con /ws (http→ws). */
function urlWebSocket(): string {
  if (import.meta.env.VITE_WS_URL) return import.meta.env.VITE_WS_URL
  const url = new URL(API_URL, window.location.href)
  url.protocol = url.protocol === "https:" ? "wss:" : "ws:"
  url.pathname = "/ws"
  url.search = ""
  return url.toString()
}

interface Manejadores {
  alRecibirMensaje: (mensaje: MensajeApi) => void
  alRecibirAviso: (aviso: AvisoApi) => void
  /** true al conectar y false al perder la conexión. */
  alCambiarEstado: (conectado: boolean) => void
}

/** Abre la conexión y devuelve una función para cerrarla. */
export function conectarTiempoReal(token: string, manejadores: Manejadores): () => void {
  const cliente = new Client({
    brokerURL: urlWebSocket(),
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 5_000,
    onConnect: () => {
      cliente.subscribe(SUSCRIPCION_MENSAJES, (frame) => {
        try {
          manejadores.alRecibirMensaje(JSON.parse(frame.body) as MensajeApi)
        } catch {
          // Un mensaje mal formado se ignora; se recuperará con el historial.
        }
      })
      cliente.subscribe(SUSCRIPCION_AVISOS, (frame) => {
        try {
          manejadores.alRecibirAviso(JSON.parse(frame.body) as AvisoApi)
        } catch {
          // Un aviso mal formado se ignora; el chat seguirá apareciendo en la lista.
        }
      })
      manejadores.alCambiarEstado(true)
    },
    onWebSocketClose: () => manejadores.alCambiarEstado(false),
    onStompError: () => manejadores.alCambiarEstado(false),
  })
  cliente.activate()
  return () => {
    void cliente.deactivate()
  }
}
