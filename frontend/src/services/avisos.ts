/**
 * Avisos de nuevos chats cercanos (RF-06). Llegan por la conexión WebSocket
 * mientras la aplicación está abierta; el aviso no da acceso por sí mismo: al
 * entrar, el servidor vuelve a comprobar distancia, plazas y caducidad (RF-07).
 */
import type { Aviso, Chat } from "../types"

/** Aviso tal como lo envía el backend (AvisoChatCercanoResponse). */
export interface AvisoApi {
  chatId: string
  nombre: string
  tema: string | null
  distanciaMaximaMetros: number
  participantes: number
  capacidad: number
  expiraEn: string | number
}

export function aAviso(aviso: AvisoApi): Aviso {
  return {
    chatId: aviso.chatId,
    title: aviso.nombre,
    topic: aviso.tema,
    distanceBand: aviso.distanciaMaximaMetros,
    participantCount: aviso.participantes,
    capacity: aviso.capacidad,
    expiresAt:
      typeof aviso.expiraEn === "number" ? aviso.expiraEn * 1000 : Date.parse(aviso.expiraEn),
  }
}

/** Chat que se abre al pulsar el aviso; todavía no se participa en él. */
export function chatDesdeAviso(aviso: Aviso): Chat {
  return {
    id: aviso.chatId,
    title: aviso.title,
    topic: aviso.topic,
    expiresAt: aviso.expiresAt,
    participantCount: aviso.participantCount,
    capacity: aviso.capacity,
    distanceBand: aviso.distanceBand,
    joined: false,
    createdByMe: false,
  }
}

/** Muestra una notificación del sistema si la app está en segundo plano y hay permiso. */
export function notificarEnSistema(aviso: Aviso, alPulsar: () => void) {
  if (typeof Notification === "undefined") return
  if (Notification.permission !== "granted" || document.visibilityState === "visible") return
  const notificacion = new Notification("Nuevo chat cerca", {
    body: `${aviso.title} · a menos de ${aviso.distanceBand} m`,
    tag: aviso.chatId,
  })
  notificacion.onclick = () => {
    window.focus()
    alPulsar()
    notificacion.close()
  }
}

/** Pide permiso para las notificaciones del sistema, si todavía no se ha decidido. */
export async function pedirPermisoNotificaciones(): Promise<NotificationPermission | "no-disponible"> {
  if (typeof Notification === "undefined") return "no-disponible"
  if (Notification.permission !== "default") return Notification.permission
  return Notification.requestPermission()
}
