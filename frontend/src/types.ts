export type Screen = "nearby" | "create" | "chat" | "settings"

export type Scenario = "50m" | "99m" | "100m" | "120m" | "invalid" | "full" | "expired"

export interface LocationSnapshot {
  distance: number
  accuracy: number
  updatedAt: number
}

export interface Message {
  id: string
  author: string
  text: string
  sentAt: number
  own?: boolean
}

export interface Chat {
  id: string
  title: string
  topic: string
  createdAt: number
  expiresAt: number
  distance: number
  participantCount: number
  participants: string[]
  messages: Message[]
  joined?: boolean
  createdByMe?: boolean
}

export interface User {
  id: string
  alias: string
  email: string
  avisosActivados: boolean
}

export interface AppNotice {
  type: "success" | "error" | "info"
  message: string
}
