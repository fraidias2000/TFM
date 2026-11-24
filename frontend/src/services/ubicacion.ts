/**
 * Obtención de la ubicación y envío al backend (RF-03, RN-03).
 * El servidor solo acepta ubicaciones con menos de 30 s y precisión de 20 m o mejor,
 * así que la aplicación la reenvía periódicamente mientras está abierta.
 */
import { apiFetch } from "./api"
import type { FuenteUbicacion } from "../types"

export interface PosicionCapturada {
  latitud: number
  longitud: number
  precision: number
  fechaCaptura: string
}

/** Envía la ubicación actual (PUT /ubicacion). */
export function enviarUbicacion(token: string, posicion: PosicionCapturada) {
  return apiFetch<{ validaHasta: string }>("/ubicacion", {
    method: "PUT",
    body: posicion,
    token,
  })
}

/** Elimina la ubicación guardada en el servidor (DELETE /ubicacion). */
export function borrarUbicacion(token: string) {
  return apiFetch<void>("/ubicacion", { method: "DELETE", token })
}

// ------------------------------------------------------------------
// Ubicación simulada para pruebas (solo en desarrollo)
// ------------------------------------------------------------------

/** Activa el selector de ubicación simulada en desarrollo o si se indica en .env. */
export const SIMULACION_DISPONIBLE =
  import.meta.env.DEV || import.meta.env.VITE_UBICACION_SIMULADA === "true"

// Punto A: Puerta del Sol (Madrid). 0,0003° de latitud son unos 33 m.
const LATITUD_A = 40.4168
const LONGITUD_A = -3.7038

const SIMULADAS: Record<
  Exclude<FuenteUbicacion, "gps">,
  { latitud: number, longitud: number, precision: number }
> = {
  "sim-origen": { latitud: LATITUD_A, longitud: LONGITUD_A, precision: 10 },
  "sim-33m": { latitud: LATITUD_A + 0.0003, longitud: LONGITUD_A, precision: 10 },
  "sim-80m": { latitud: LATITUD_A + 0.00072, longitud: LONGITUD_A, precision: 10 },
  "sim-120m": { latitud: LATITUD_A + 0.00108, longitud: LONGITUD_A, precision: 10 },
  "sim-imprecisa": { latitud: LATITUD_A, longitud: LONGITUD_A, precision: 35 },
}

export const OPCIONES_UBICACION: {
  id: FuenteUbicacion
  label: string
  short: string
}[] = [
  { id: "gps", label: "GPS del dispositivo", short: "GPS" },
  { id: "sim-origen", label: "Punto A (Puerta del Sol)", short: "Punto A" },
  { id: "sim-33m", label: "A 33 m del punto A", short: "33 m" },
  { id: "sim-80m", label: "A 80 m del punto A", short: "80 m" },
  { id: "sim-120m", label: "A 120 m del punto A", short: "120 m" },
  { id: "sim-imprecisa", label: "Punto A con precisión de 35 m", short: "Imprecisa" },
]

const CLAVE_FUENTE = "hablabar.ubicacion"

export function leerFuenteGuardada(): FuenteUbicacion {
  if (!SIMULACION_DISPONIBLE) return "gps"
  const guardada = localStorage.getItem(CLAVE_FUENTE)
  return OPCIONES_UBICACION.some((opcion) => opcion.id === guardada)
    ? (guardada as FuenteUbicacion)
    : "sim-origen"
}

export function guardarFuente(fuente: FuenteUbicacion) {
  localStorage.setItem(CLAVE_FUENTE, fuente)
}

/** Obtiene la posición actual, real o simulada. */
export function capturarPosicion(
  fuente: FuenteUbicacion,
): Promise<PosicionCapturada> {
  if (fuente !== "gps") {
    return Promise.resolve({
      ...SIMULADAS[fuente],
      fechaCaptura: new Date().toISOString(),
    })
  }

  return new Promise((resolve, reject) => {
    if (!("geolocation" in navigator)) {
      reject(new Error("Este navegador no permite obtener la ubicación."))
      return
    }
    navigator.geolocation.getCurrentPosition(
      (posicion) =>
        resolve({
          latitud: posicion.coords.latitude,
          longitud: posicion.coords.longitude,
          precision: posicion.coords.accuracy,
          fechaCaptura: new Date(posicion.timestamp).toISOString(),
        }),
      (error) => reject(new Error(mensajeGeolocalizacion(error.code))),
      { enableHighAccuracy: true, maximumAge: 0, timeout: 15_000 },
    )
  })
}

function mensajeGeolocalizacion(codigo: number): string {
  if (codigo === 1) {
    return "Has denegado el permiso de ubicación. Actívalo en el navegador para crear o descubrir chats."
  }
  if (codigo === 3) return "La ubicación está tardando demasiado. Inténtalo de nuevo."
  return "No se puede obtener la ubicación del dispositivo."
}