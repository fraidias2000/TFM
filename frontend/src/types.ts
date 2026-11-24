export type Screen = "nearby" | "create" | "chat" | "settings"

export interface Message {
  id: string
  author: string
  text: string
  sentAt: number
  own?: boolean
}

/** Chat tal como lo usa la interfaz, construido a partir de las respuestas del backend. */
export interface Chat {
  id: string
  title: string
  topic: string | null
  /** Fecha de caducidad fijada por el servidor (milisegundos). */
  expiresAt: number
  participantCount: number
  capacity: number
  /** Tramo de distancia (menos de 25, 50 o 100 m). Solo en los chats cercanos. */
  distanceBand?: number
  joined: boolean
  createdByMe: boolean
}

/** Participante visible dentro de un chat: solo su alias (RF-09). */
export interface Participant {
  id: string
  alias: string
  isCreator: boolean
}

export interface User {
  id: string
  alias: string
  email: string
  avisosActivados: boolean
}

/** Origen de la ubicación: el GPS real o una posición simulada para las pruebas. */
export type FuenteUbicacion =
  | "gps"
  | "sim-origen"
  | "sim-33m"
  | "sim-80m"
  | "sim-120m"
  | "sim-imprecisa"

/** Estado de la última ubicación enviada al servidor. */
export interface EstadoUbicacion {
  estado: "buscando" | "lista" | "error"
  precision?: number
  mensaje?: string
}