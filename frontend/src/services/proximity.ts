/**
 * Utilidades de presentación. Las reglas de distancia, capacidad, caducidad y
 * validez de la ubicación las aplica el backend.
 */

export function minutesRemaining(expiresAt: number, now = Date.now()): number {
  return Math.max(0, Math.ceil((expiresAt - now) / 60_000))
}

export function formatTime(timestamp: number): string {
  return new Intl.DateTimeFormat("es", {
    hour: "2-digit",
    minute: "2-digit",
  }).format(timestamp)
}