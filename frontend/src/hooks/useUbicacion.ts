import { useCallback, useEffect, useRef, useState } from "react"
import { capturarPosicion, enviarUbicacion } from "../services/ubicacion"
import type { EstadoUbicacion, FuenteUbicacion } from "../types"

/** Cada cuánto se reenvía la ubicación. El servidor la considera válida 30 s. */
const INTERVALO_ENVIO = 20_000

/**
 * Mantiene actualizada en el servidor la ubicación del usuario mientras hay sesión.
 * asegurar() garantiza que hay una ubicación reciente antes de crear, descubrir o
 * entrar en un chat.
 */
export function useUbicacion(token: string | null, fuente: FuenteUbicacion) {
  const [estado, setEstado] = useState<EstadoUbicacion>({ estado: "buscando" })
  const ultimoEnvio = useRef(0)
  const enCurso = useRef<Promise<boolean> | null>(null)
  const tokenRef = useRef(token)
  const fuenteRef = useRef(fuente)
  tokenRef.current = token
  fuenteRef.current = fuente

  const enviar = useCallback((): Promise<boolean> => {
    if (enCurso.current) return enCurso.current
    const tokenActual = tokenRef.current
    if (!tokenActual) return Promise.resolve(false)

    const promesa = (async () => {
      try {
        const posicion = await capturarPosicion(fuenteRef.current)
        await enviarUbicacion(tokenActual, posicion)
        ultimoEnvio.current = Date.now()
        setEstado({ estado: "lista", precision: Math.round(posicion.precision) })
        return true
      } catch (error) {
        ultimoEnvio.current = 0
        setEstado({
          estado: "error",
          mensaje:
            error instanceof Error
              ? error.message
              : "No se ha podido actualizar la ubicación.",
        })
        // Si el token ya no es válido (401), api.ts cierra la sesión.
        return false
      } finally {
        enCurso.current = null
      }
    })()
    enCurso.current = promesa
    return promesa
  }, [])

  /** Devuelve true si hay una ubicación válida en el servidor; si no, la envía. */
  const asegurar = useCallback(async (): Promise<boolean> => {
    if (ultimoEnvio.current > 0 && Date.now() - ultimoEnvio.current < INTERVALO_ENVIO) {
      return true
    }
    return enviar()
  }, [enviar])

  // Envía la ubicación al iniciar sesión o al cambiar de fuente, y después cada 20 s.
  useEffect(() => {
    ultimoEnvio.current = 0
    if (!token) {
      setEstado({ estado: "buscando" })
      return
    }
    setEstado({ estado: "buscando" })
    void (enCurso.current ?? Promise.resolve()).then(() => enviar())
    const intervalo = window.setInterval(() => void enviar(), INTERVALO_ENVIO)
    return () => window.clearInterval(intervalo)
  }, [token, fuente, enviar])

  return { ubicacion: estado, actualizarUbicacion: enviar, asegurarUbicacion: asegurar }
}