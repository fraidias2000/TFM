/**
 * Envío y consulta de mensajes (RF-08, RN-07). El servidor comprueba que el chat
 * está activo y que el usuario participa; aquí solo se adaptan las respuestas.
 */
import { apiFetch } from "./api"
import type { Message } from "../types"

/** Mensaje tal como lo devuelve el backend (MensajeResponse). */
export interface MensajeApi {
  id: string
  idCliente: string
  chatId: string
  autorId: string
  alias: string
  contenido: string
  fechaEnvio: string | number
}

/** Convierte la fecha del servidor (texto ISO o segundos) en milisegundos. */
function aMilisegundos(fecha: string | number): number {
  return typeof fecha === "number" ? fecha * 1000 : Date.parse(fecha)
}

export function aMensaje(mensaje: MensajeApi, usuarioId: string): Message {
  return {
    id: mensaje.id,
    author: mensaje.alias,
    text: mensaje.contenido,
    sentAt: aMilisegundos(mensaje.fechaEnvio),
    own: mensaje.autorId === usuarioId,
  }
}

export function enviarMensaje(
  token: string,
  chatId: string,
  idCliente: string,
  contenido: string,
) {
  return apiFetch<MensajeApi>(`/chats/${chatId}/mensajes`, {
    method: "POST",
    body: { idCliente, contenido },
    token,
  })
}

export function historialMensajes(token: string, chatId: string) {
  return apiFetch<MensajeApi[]>(`/chats/${chatId}/mensajes`, { token })
}

/**
 * Identificador único de un mensaje, generado en el dispositivo. Si el envío
 * falla y se repite, se reutiliza para que el servidor no lo duplique (RNF-06).
 */
export function nuevoIdCliente(): string {
  if (typeof crypto.randomUUID === "function") return crypto.randomUUID()
  // crypto.randomUUID solo existe en contextos seguros (https o localhost).
  const bytes = crypto.getRandomValues(new Uint8Array(16))
  bytes[6] = (bytes[6] & 0x0f) | 0x40
  bytes[8] = (bytes[8] & 0x3f) | 0x80
  const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, "0")).join("")
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}

/** Añade mensajes a una lista sin duplicarlos y en orden de envío (RF-14). */
export function fusionar(actuales: Message[], nuevos: Message[]): Message[] {
  const porId = new Map(actuales.map((mensaje) => [mensaje.id, mensaje]))
  for (const mensaje of nuevos) porId.set(mensaje.id, mensaje)
  return [...porId.values()].sort((a, b) => a.sentAt - b.sentAt)
}
