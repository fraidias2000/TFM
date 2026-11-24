/**
 * Operaciones de chats contra el backend (RF-04, RF-05, RF-07, RF-09, RF-11 a RF-14).
 * Las comprobaciones de distancia, capacidad y caducidad las hace el servidor;
 * aquí solo se traducen las respuestas al modelo de la interfaz.
 */
import { apiFetch } from "./api"
import type { Chat, Participant } from "../types"

interface ChatResponse {
  id: string
  nombre: string
  tema: string | null
  participantes: number
  capacidad: number
  creadoEn: string
  expiraEn: string
  creadoPorMi: boolean
  participando: boolean
  fechaEntrada: string
}

interface ChatCercanoResponse {
  id: string
  nombre: string
  tema: string | null
  participantes: number
  capacidad: number
  expiraEn: string
  distanciaMaximaMetros: number
  participando: boolean
}

interface ParticipanteResponse {
  id: string
  alias: string
  esCreador: boolean
}

function desdeChat(chat: ChatResponse): Chat {
  return {
    id: chat.id,
    title: chat.nombre,
    topic: chat.tema,
    expiresAt: Date.parse(chat.expiraEn),
    participantCount: chat.participantes,
    capacity: chat.capacidad,
    joined: chat.participando,
    createdByMe: chat.creadoPorMi,
  }
}

function desdeCercano(chat: ChatCercanoResponse): Chat {
  return {
    id: chat.id,
    title: chat.nombre,
    topic: chat.tema,
    expiresAt: Date.parse(chat.expiraEn),
    participantCount: chat.participantes,
    capacity: chat.capacidad,
    distanceBand: chat.distanciaMaximaMetros,
    joined: chat.participando,
    createdByMe: false,
  }
}

export async function crearChat(
  token: string,
  nombre: string,
  tema: string | null,
): Promise<Chat> {
  const chat = await apiFetch<ChatResponse>("/chats", {
    method: "POST",
    body: { nombre, tema },
    token,
  })
  return desdeChat(chat)
}

export async function chatsCercanos(token: string): Promise<Chat[]> {
  const chats = await apiFetch<ChatCercanoResponse[]>("/chats/cercanos", { token })
  return chats.map(desdeCercano)
}

export async function chatsParticipando(token: string): Promise<Chat[]> {
  const chats = await apiFetch<ChatResponse[]>("/chats/participando", { token })
  return chats.map(desdeChat)
}

export async function detalleChat(token: string, chatId: string): Promise<Chat> {
  return desdeChat(await apiFetch<ChatResponse>(`/chats/${chatId}`, { token }))
}

export async function participantesChat(
  token: string,
  chatId: string,
): Promise<Participant[]> {
  const lista = await apiFetch<ParticipanteResponse[]>(
    `/chats/${chatId}/participantes`,
    { token },
  )
  return lista.map((p) => ({ id: p.id, alias: p.alias, isCreator: p.esCreador }))
}

export async function entrarChat(token: string, chatId: string): Promise<Chat> {
  const chat = await apiFetch<ChatResponse>(`/chats/${chatId}/participacion`, {
    method: "POST",
    token,
  })
  return desdeChat(chat)
}

export function salirChat(token: string, chatId: string) {
  return apiFetch<void>(`/chats/${chatId}/participacion`, {
    method: "DELETE",
    token,
  })
}