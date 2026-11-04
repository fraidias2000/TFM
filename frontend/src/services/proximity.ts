import type { Chat, LocationSnapshot, Scenario } from "../types"

export const MAX_DISTANCE = 100
export const MAX_LOCATION_AGE = 30_000
export const MAX_ACCURACY = 20
export const MAX_PARTICIPANTS = 20
export const CHAT_LIFETIME = 60 * 60_000

export function validateLocation(location: LocationSnapshot): string | null {
  const age = Date.now() - location.updatedAt
  if (age > MAX_LOCATION_AGE || location.accuracy > MAX_ACCURACY) {
    return "Actualiza tu ubicación. Debe tener menos de 30 segundos y una precisión de 20 m o mejor."
  }
  return null
}

export function canEnterChat(
  chat: Chat,
  location: LocationSnapshot,
  scenario: Scenario,
): string | null {
  const locationError = validateLocation(location)
  if (locationError) return locationError
  if (scenario === "expired" || Date.now() >= chat.expiresAt) {
    return "Este chat ha caducado. Su historial ya no está disponible."
  }
  if (scenario === "full" || chat.participantCount >= MAX_PARTICIPANTS) {
    return "Este chat está completo. Ya participan 20 personas."
  }
  if (location.distance >= MAX_DISTANCE) {
    return `Estás a ${location.distance} m. Debes estar a menos de 100 m para entrar.`
  }
  return null
}

export function minutesRemaining(expiresAt: number): number {
  return Math.max(0, Math.ceil((expiresAt - Date.now()) / 60_000))
}

export function formatTime(timestamp: number): string {
  return new Intl.DateTimeFormat("es", {
    hour: "2-digit",
    minute: "2-digit",
  }).format(timestamp)
}
